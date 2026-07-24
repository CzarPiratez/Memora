package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Durable PDF extraction header. Immutable key includes fingerprint and schema so
 * superseded provenance (ADR-022) can coexist with the current Asset fingerprint.
 */
@Entity(
    tableName = "pdf_extractions",
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
data class PdfExtractionEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "text_coverage") val textCoverage: String,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "extracted_at_epoch_millis") val extractedAtEpochMillis: Long,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "integrity") val integrity: String,
)

@Entity(
    tableName = "pdf_extraction_pages",
    primaryKeys = [
        "source_id",
        "source_asset_key",
        "fingerprint",
        "schema_version",
        "page_number",
    ],
    foreignKeys = [
        ForeignKey(
            entity = PdfExtractionEntity::class,
            parentColumns = [
                "source_id",
                "source_asset_key",
                "fingerprint",
                "schema_version",
            ],
            childColumns = [
                "source_id",
                "source_asset_key",
                "fingerprint",
                "schema_version",
            ],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["source_id", "source_asset_key", "fingerprint", "schema_version"],
        ),
    ],
)
data class PdfExtractionPageEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "page_text") val pageText: String,
)

@Entity(
    tableName = "pdf_extraction_metadata",
    primaryKeys = [
        "source_id",
        "source_asset_key",
        "fingerprint",
        "schema_version",
        "metadata_name",
    ],
    foreignKeys = [
        ForeignKey(
            entity = PdfExtractionEntity::class,
            parentColumns = [
                "source_id",
                "source_asset_key",
                "fingerprint",
                "schema_version",
            ],
            childColumns = [
                "source_id",
                "source_asset_key",
                "fingerprint",
                "schema_version",
            ],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["source_id", "source_asset_key", "fingerprint", "schema_version"],
        ),
    ],
)
data class PdfExtractionMetadataEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "metadata_name") val metadataName: String,
    @ColumnInfo(name = "metadata_value") val metadataValue: String,
)
