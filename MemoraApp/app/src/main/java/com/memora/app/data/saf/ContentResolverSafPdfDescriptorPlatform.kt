package com.memora.app.data.saf

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.memora.app.domain.asset.AssetFingerprint

/**
 * Android implementation of the descriptor broker's narrow platform operations.
 *
 * It has no parser, Room, source repository, or UI dependency. The broker validates
 * the exact source and fresh grant before calling this adapter. API 26-28 are denied
 * safely until their non-heuristic membership fallback has its own accepted test
 * fixture; no path-prefix substitute is permitted.
 */
internal class ContentResolverSafPdfDescriptorPlatform(
    context: Context,
) : SafPdfDescriptorPlatform {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    override fun membership(target: SafPdfCanonicalTarget): SafPdfTreeMembership {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return SafPdfTreeMembership.UNSUPPORTED
        }
        return try {
            val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
                target.treeUri,
                target.treeDocumentId,
            )
            if (DocumentsContract.isChildDocument(resolver, rootDocumentUri, target.documentUri)) {
                SafPdfTreeMembership.VERIFIED
            } else {
                SafPdfTreeMembership.REJECTED
            }
        } catch (_: SecurityException) {
            SafPdfTreeMembership.UNAVAILABLE
        } catch (_: IllegalArgumentException) {
            SafPdfTreeMembership.UNAVAILABLE
        } catch (_: RuntimeException) {
            SafPdfTreeMembership.UNAVAILABLE
        }
    }

    override fun observeFingerprint(target: SafPdfCanonicalTarget): AssetFingerprint? {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        return try {
            resolver.query(target.documentUri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }
                val documentIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                if (documentIdIndex < 0 || mimeIndex < 0) {
                    return@use null
                }
                val documentId = cursor.getString(documentIdIndex) ?: return@use null
                val mimeType = cursor.getString(mimeIndex) ?: return@use null
                val lastModified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                    cursor.getLong(modifiedIndex)
                } else {
                    null
                }
                val sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex)
                } else {
                    null
                }
                SafPdfDocumentFingerprint.from(
                    documentId = documentId,
                    lastModifiedEpochMillis = lastModified,
                    sizeBytes = sizeBytes,
                    mimeType = mimeType,
                )
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun openReadOnly(
        target: SafPdfCanonicalTarget,
        cancellationSignal: CancellationSignal?,
    ): ParcelFileDescriptor? = resolver.openFileDescriptor(
        target.documentUri,
        "r",
        cancellationSignal,
    )
}
