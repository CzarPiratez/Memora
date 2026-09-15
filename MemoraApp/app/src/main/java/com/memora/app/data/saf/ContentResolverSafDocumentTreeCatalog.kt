package com.memora.app.data.saf

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [SafDocumentTreeCatalog].
 *
 * It queries immediate-child metadata for one requested folder through the persisted
 * tree URI.
 *
 * ## Paging is done here, not by the provider
 *
 * The sort, selection and limit passed in [queryArguments] are **hints**.
 * `DocumentsProvider.query` forwards only the projection to
 * `queryChildDocuments` unless a provider opts in, and the providers that matter
 * on a phone — ExternalStorageProvider, Drive, OneDrive — do not. Every one of
 * them therefore returns the folder's children from the beginning, in whatever
 * order they like, however many there are.
 *
 * The original code already enforced the *limit* here for that reason but
 * trusted the provider for the *cursor*, so `documentId > afterDocumentId` was
 * dropped on the floor: page two returned page one again, `hasMore` stayed
 * true, and the discovery worker re-enqueued itself forever once a folder held
 * more than one page of children. Order and cursor are now applied on this
 * side too, so a page is a real page for any provider.
 */
class ContentResolverSafDocumentTreeCatalog(
    context: Context,
) : SafDocumentTreeCatalog {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    override suspend fun readChildMetadataPage(
        treeUri: String,
        parentDocumentId: String?,
        afterDocumentId: String?,
        limit: Int,
    ): SafDocumentTreeMetadataPage = withContext(Dispatchers.IO) {
        require(limit > 0) { "A SAF metadata limit must be positive." }

        val parsedTreeUri = Uri.parse(treeUri)
        val treeDocumentId = DocumentsContract.getTreeDocumentId(parsedTreeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            parsedTreeUri,
            parentDocumentId ?: treeDocumentId,
        )
        val children = resolver.query(
            childrenUri,
            projection(),
            queryArguments(afterDocumentId, limit + 1),
            null,
        )?.use { cursor ->
            cursor.readAtMost(MAX_CHILDREN_PER_FOLDER, parsedTreeUri)
        }.orEmpty()

        // A provider that honoured the hints has already done all three of
        // these; repeating them is cheap and is the only thing that makes the
        // page correct on a provider that did not.
        val remaining = children
            .distinctBy(SafDocumentMetadata::documentId)
            .sortedBy(SafDocumentMetadata::documentId)
            .let { ordered ->
                if (afterDocumentId == null) {
                    ordered
                } else {
                    ordered.filter { it.documentId > afterDocumentId }
                }
            }

        SafDocumentTreeMetadataPage(
            documents = remaining.take(limit),
            hasMore = remaining.size > limit,
        )
    }

    private fun projection(): Array<String> = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    private fun queryArguments(afterDocumentId: String?, limit: Int): Bundle = Bundle().apply {
        putString(
            ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
            "${DocumentsContract.Document.COLUMN_DOCUMENT_ID} ASC",
        )
        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        if (afterDocumentId != null) {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${DocumentsContract.Document.COLUMN_DOCUMENT_ID} > ?",
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(afterDocumentId),
            )
        }
    }

    private fun Cursor.readAtMost(
        limit: Int,
        treeUri: Uri,
    ): List<SafDocumentMetadata> = buildList {
        while (size < limit && moveToNext()) {
            val documentId = getString(columnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
            add(
                SafDocumentMetadata(
                    documentId = documentId,
                    documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString(),
                    mimeType = getString(columnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)),
                    displayName = getStringOrNull(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    sizeBytes = getLongOrNull(DocumentsContract.Document.COLUMN_SIZE),
                    lastModifiedEpochMillis = getLongOrNull(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                )
            )
        }
    }

    private fun Cursor.columnIndex(column: String): Int = getColumnIndexOrThrow(column)

    private fun Cursor.getStringOrNull(column: String): String? {
        val index = columnIndex(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getLongOrNull(column: String): Long? {
        val index = columnIndex(column)
        return if (isNull(index)) null else getLong(index)
    }

    private companion object {
        /**
         * Ordering and cursoring a page needs the folder's whole child listing,
         * which is what the provider hands over anyway. This only stops a
         * pathological folder from being read into memory without bound; a real
         * one is orders of magnitude below it.
         */
        const val MAX_CHILDREN_PER_FOLDER = 20_000
    }
}
