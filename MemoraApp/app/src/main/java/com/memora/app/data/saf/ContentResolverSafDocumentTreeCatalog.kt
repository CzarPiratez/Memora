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
 * tree URI. The requested limit is supplied to the provider and enforced again while
 * consuming the cursor, so Memora never exposes an unbounded page even if a provider
 * ignores the optional query hint.
 */
class ContentResolverSafDocumentTreeCatalog(
    context: Context,
) : SafDocumentTreeCatalog {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    override suspend fun hasPersistedReadAccess(treeUri: String): Boolean = withContext(Dispatchers.IO) {
        val requestedUri = Uri.parse(treeUri)
        resolver.persistedUriPermissions.any { permission ->
            permission.uri == requestedUri && permission.isReadPermission
        }
    }

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
        val rows = resolver.query(
            childrenUri,
            projection(),
            queryArguments(afterDocumentId, limit + 1),
            null,
        )?.use { cursor ->
            cursor.readAtMost(limit + 1, parsedTreeUri)
        }.orEmpty()

        SafDocumentTreeMetadataPage(
            documents = rows.take(limit),
            hasMore = rows.size > limit,
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
}
