package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeApproval
import java.time.Instant

/** Room representation of one SAF tree reference owned by Memora. */
@Entity(tableName = "document_tree_approvals")
data class DocumentTreeApprovalEntity(
    @PrimaryKey
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "tree_uri") val treeUri: String,
    @ColumnInfo(name = "approved_at_epoch_millis") val approvedAtEpochMillis: Long,
)

internal fun DocumentTreeApproval.toEntity(): DocumentTreeApprovalEntity =
    DocumentTreeApprovalEntity(
        sourceId = sourceId.value,
        treeUri = treeUri,
        approvedAtEpochMillis = approvedAt.toEpochMilli(),
    )

internal fun DocumentTreeApprovalEntity.toDomain(): DocumentTreeApproval =
    DocumentTreeApproval(
        sourceId = SourceId(sourceId),
        treeUri = treeUri,
        approvedAt = Instant.ofEpochMilli(approvedAtEpochMillis),
    )
