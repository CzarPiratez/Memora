package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Durable image EXIF header. Key includes fingerprint and schema so superseded
 * provenance can coexist with the current Asset fingerprint (ADR-022 pattern).
 */
@Entity(
    tableName = "image_exif_extractions",
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
data class ImageExifExtractionEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "asset_kind") val assetKind: String,
    @ColumnInfo(name = "datetime_original") val datetimeOriginal: String?,
    @ColumnInfo(name = "image_width") val imageWidth: Int?,
    @ColumnInfo(name = "image_height") val imageHeight: Int?,
    @ColumnInfo(name = "orientation") val orientation: Int?,
    @ColumnInfo(name = "make") val make: String?,
    @ColumnInfo(name = "model") val model: String?,
    @ColumnInfo(name = "extracted_at_epoch_millis") val extractedAtEpochMillis: Long,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "integrity") val integrity: String,
)
