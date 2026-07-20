package com.memora.app.data.local

import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository

/** Room-backed private storage for SAF document-tree references. */
class RoomDocumentTreeApprovalRepository(
    private val approvalDao: DocumentTreeApprovalDao,
) : DocumentTreeApprovalRepository {
    override suspend fun save(approval: DocumentTreeApproval) {
        approvalDao.upsert(approval.toEntity())
    }

    override suspend fun findAll(): List<DocumentTreeApproval> = approvalDao
        .findAll()
        .map(DocumentTreeApprovalEntity::toDomain)
}
