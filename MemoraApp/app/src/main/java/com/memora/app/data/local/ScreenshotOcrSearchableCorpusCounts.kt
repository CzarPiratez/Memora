package com.memora.app.data.local

import androidx.room.ColumnInfo

/** Current-fingerprint screenshot OCR counts keyword search can read (ADR-022). */
data class ScreenshotOcrSearchableCorpusCounts(
    @ColumnInfo(name = "screenshot_count") val screenshotCount: Int,
)
