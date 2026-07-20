package com.memora.app.data.saf

/**
 * Narrow platform boundary for a persisted Android document tree.
 *
 * Implementations may query document metadata, but must never open a document or
 * request broader storage access. The continuation token is private to the platform
 * adapter and is intentionally opaque to the discovery domain.
 */
interface SafDocumentTreeCatalog {
    suspend fun hasPersistedReadAccess(treeUri: String): Boolean

    suspend fun readChildMetadataPage(
        treeUri: String,
        afterDocumentId: String?,
        limit: Int,
    ): SafDocumentTreeMetadataPage
}

/** Metadata from a DocumentsProvider row. No PDF contents are represented here. */
data class SafDocumentMetadata(
    val documentId: String,
    val documentUri: String,
    val mimeType: String,
    val displayName: String?,
    val sizeBytes: Long?,
    val lastModifiedEpochMillis: Long?,
) {
    init {
        require(documentId.isNotBlank()) { "A SAF document ID cannot be blank." }
        require(documentUri.isNotBlank()) { "A SAF document URI cannot be blank." }
        require(mimeType.isNotBlank()) { "A SAF document MIME type cannot be blank." }
        require(sizeBytes == null || sizeBytes >= 0) { "A SAF document size cannot be negative." }
        require(lastModifiedEpochMillis == null || lastModifiedEpochMillis >= 0) {
            "A SAF document modified time cannot be negative."
        }
    }
}

/** A bounded immediate-child metadata page returned by one approved document tree. */
data class SafDocumentTreeMetadataPage(
    val documents: List<SafDocumentMetadata>,
    val hasMore: Boolean,
) {
    init {
        require(documents.map(SafDocumentMetadata::documentId).distinct().size == documents.size) {
            "A SAF metadata page cannot contain duplicate document IDs."
        }
    }
}
