package com.memora.app.data.local

import androidx.room.ColumnInfo

/** One current-fingerprint PDF page row that matched a keyword query. */
data class PdfExtractionPageSearchRow(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "page_text") val pageText: String,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "display_name") val displayName: String?,
)
