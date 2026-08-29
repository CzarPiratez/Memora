package com.memora.app.data.local

import androidx.room.ColumnInfo

/** Room projection for READY Memory-evidence corpus inventory (MIG-07 readiness). */
data class MemoryEvidenceCorpusCountRow(
    @ColumnInfo(name = "evidence_count") val evidenceCount: Int,
    @ColumnInfo(name = "document_count") val documentCount: Int,
)
