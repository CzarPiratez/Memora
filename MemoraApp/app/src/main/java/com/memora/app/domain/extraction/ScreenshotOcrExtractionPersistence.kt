package com.memora.app.domain.extraction

/** Persistence boundary for durable screenshot OCR extraction rows. */
interface ScreenshotOcrExtractionPersistence {
    suspend fun findHeader(record: ScreenshotOcrExtractionRecord): ScreenshotOcrExtractionRecord?

    suspend fun insert(record: ScreenshotOcrExtractionRecord)

    suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int
}
