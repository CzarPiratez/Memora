package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "memories",
    primaryKeys = ["revision_id"],
    indices = [
        Index(value = ["memory_id"]),
        Index(
            value = ["source_id", "source_asset_key", "fingerprint", "assembly_schema_version"],
            unique = true,
        ),
        Index(value = ["source_id", "source_asset_key"]),
        Index(value = ["fingerprint"]),
    ],
)
data class MemoryEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "memory_id") val memoryId: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "assembly_schema_version") val assemblySchemaVersion: String,
    @ColumnInfo(name = "integrity_state") val integrityState: String,
    @ColumnInfo(name = "summary_text") val summaryText: String,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "memory_extraction_schemas",
    primaryKeys = ["revision_id", "schema_version"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["revision_id"],
            childColumns = ["revision_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["revision_id"])],
)
data class MemoryExtractionSchemaEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
)

@Entity(
    tableName = "memory_evidence",
    primaryKeys = ["revision_id", "evidence_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["revision_id"],
            childColumns = ["revision_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["revision_id"])],
)
data class MemoryEvidenceEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "evidence_id") val evidenceId: String,
    @ColumnInfo(name = "evidence_kind") val evidenceKind: String,
    @ColumnInfo(name = "evidence_class") val evidenceClass: String,
    @ColumnInfo(name = "locator") val locator: String,
    @ColumnInfo(name = "excerpt") val excerpt: String,
)

@Entity(
    tableName = "memory_anchors",
    primaryKeys = ["revision_id", "anchor_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["revision_id"],
            childColumns = ["revision_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["revision_id"])],
)
data class MemoryAnchorEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "anchor_id") val anchorId: String,
    @ColumnInfo(name = "anchor_kind") val anchorKind: String,
    @ColumnInfo(name = "anchor_text") val anchorText: String,
)

@Entity(
    tableName = "memory_anchor_evidence",
    primaryKeys = ["revision_id", "anchor_id", "evidence_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryAnchorEntity::class,
            parentColumns = ["revision_id", "anchor_id"],
            childColumns = ["revision_id", "anchor_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemoryEvidenceEntity::class,
            parentColumns = ["revision_id", "evidence_id"],
            childColumns = ["revision_id", "evidence_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["revision_id", "anchor_id"]),
        Index(value = ["revision_id", "evidence_id"]),
    ],
)
data class MemoryAnchorEvidenceEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "anchor_id") val anchorId: String,
    @ColumnInfo(name = "evidence_id") val evidenceId: String,
)

@Entity(
    tableName = "memory_summary_evidence",
    primaryKeys = ["revision_id", "evidence_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["revision_id"],
            childColumns = ["revision_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemoryEvidenceEntity::class,
            parentColumns = ["revision_id", "evidence_id"],
            childColumns = ["revision_id", "evidence_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["revision_id"]),
        Index(value = ["revision_id", "evidence_id"]),
    ],
)
data class MemorySummaryEvidenceEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "evidence_id") val evidenceId: String,
)
