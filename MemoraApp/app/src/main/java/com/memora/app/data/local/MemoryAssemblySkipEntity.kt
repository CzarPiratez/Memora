package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Durable skip for an Asset that cannot become a Memory for this assembly schema
 * until its fact set changes (D-9).
 */
@Entity(
    tableName = "memory_assembly_skips",
    primaryKeys = [
        "source_id",
        "source_asset_key",
        "fingerprint",
        "assembly_schema_version",
    ],
    indices = [
        Index(value = ["source_id", "source_asset_key", "fingerprint"]),
    ],
)
data class MemoryAssemblySkipEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "assembly_schema_version") val assemblySchemaVersion: String,
    @ColumnInfo(name = "reason") val reason: String,
    @ColumnInfo(name = "facts_digest") val factsDigest: String,
    @ColumnInfo(name = "recorded_at_epoch_millis") val recordedAtEpochMillis: Long,
)
