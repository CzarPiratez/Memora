package com.memora.app.data.local

import androidx.room.ColumnInfo

/** One current-fingerprint note page row that matched a keyword query. */
data class NotePageExtractionSearchRow(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "full_text") val fullText: String,
    @ColumnInfo(name = "display_name") val displayName: String?,
)
