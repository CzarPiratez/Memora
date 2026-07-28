package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Durable screenshot OCR text. Key includes fingerprint and schema so superseded
 * provenance can coexist with the current Asset fingerprint (ADR-022 pattern).
 */
@Entity(
    tableName = "screenshot_ocr_extractions",
    primaryKeys = [
        "source_id",
        "source_asset_key",
        "fingerprint",
        "schema_version",
    ],
    indices = [
        Index(value = ["source_id", "source_asset_key"]),
        Index(value = ["fingerprint"]),
    ],
)
data class ScreenshotOcrExtractionEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "full_text") val fullText: String,
    @ColumnInfo(name = "text_truncated") val textTruncated: Boolean,
    @ColumnInfo(name = "engine_id") val engineId: String,
    @ColumnInfo(name = "engine_version") val engineVersion: String,
    @ColumnInfo(name = "extracted_at_epoch_millis") val extractedAtEpochMillis: Long,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "integrity") val integrity: String,
)
