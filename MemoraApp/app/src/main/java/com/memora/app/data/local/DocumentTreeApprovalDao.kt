package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DocumentTreeApprovalDao {
    @Upsert
    suspend fun upsert(approval: DocumentTreeApprovalEntity)

    @Query(
        """
        SELECT * FROM document_tree_approvals
        WHERE source_id = :sourceId
        LIMIT 1
        """,
    )
    suspend fun find(sourceId: String): DocumentTreeApprovalEntity?

    @Query(
        """
        SELECT * FROM document_tree_approvals
        ORDER BY approved_at_epoch_millis ASC, source_id ASC
        """,
    )
    suspend fun findAll(): List<DocumentTreeApprovalEntity>
}
