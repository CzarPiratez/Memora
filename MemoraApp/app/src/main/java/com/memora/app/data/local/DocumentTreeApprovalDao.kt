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
        ORDER BY approved_at_epoch_millis ASC, source_id ASC
        """,
    )
    suspend fun findAll(): List<DocumentTreeApprovalEntity>
}
