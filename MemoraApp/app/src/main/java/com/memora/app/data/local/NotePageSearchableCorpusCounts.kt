package com.memora.app.data.local

import androidx.room.ColumnInfo

/** Current-fingerprint note page counts keyword search can read (ADR-022). */
data class NotePageSearchableCorpusCounts(
    @ColumnInfo(name = "note_count") val noteCount: Int,
)
