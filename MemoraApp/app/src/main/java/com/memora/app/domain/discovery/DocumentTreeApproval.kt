package com.memora.app.domain.discovery

import com.memora.app.domain.asset.SourceId
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

    suspend fun findAll(): List<DocumentTreeApproval>
}
