package com.memora.app.data.local

import androidx.room.ColumnInfo

/** Current-fingerprint page/document counts keyword search can read (ADR-022). */
data class PdfSearchableCorpusCounts(
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "document_count") val documentCount: Int,
)
