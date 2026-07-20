package com.memora.app.domain.discovery

import com.memora.app.domain.asset.SourceId
import java.security.MessageDigest
import java.time.Instant

/**
 * A private record of one document tree the user approved through Android's Storage
 * Access Framework. This is a source reference, never a document or a copy of one.
 */
data class DocumentTreeApproval(
    val sourceId: SourceId,
    val treeUri: String,
    val approvedAt: Instant,
) {
    init {
        require(treeUri.isNotBlank()) { "An approved document tree URI cannot be blank." }
    }
}

/**
 * Durable Memora-owned references to user-approved document trees.
 *
 * Android remains the authority for the corresponding persisted URI grant. A future
 * SAF source must verify that platform grant before discovering any PDF.
 */
interface DocumentTreeApprovalRepository {
    suspend fun save(approval: DocumentTreeApproval)

    /** Finds one exact approved source without exposing or selecting its tree URI. */
    suspend fun find(sourceId: SourceId): DocumentTreeApproval? = findAll()
        .firstOrNull { approval -> approval.sourceId == sourceId }

    suspend fun findAll(): List<DocumentTreeApproval>
}

/** Creates a stable private identity for one Android SAF document-tree reference. */
object DocumentTreeSource {
    private const val SOURCE_ID_PREFIX = "android-saf-document-tree:"

    fun approvalFor(
        persistedTreeUri: String,
        approvedAt: Instant,
    ): DocumentTreeApproval = DocumentTreeApproval(
        sourceId = sourceIdFor(persistedTreeUri),
        treeUri = persistedTreeUri,
        approvedAt = approvedAt,
    )

    fun sourceIdFor(persistedTreeUri: String): SourceId {
        require(persistedTreeUri.isNotBlank()) {
            "An approved document tree URI cannot be blank."
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(persistedTreeUri.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

        return SourceId(SOURCE_ID_PREFIX + digest)
    }
}
