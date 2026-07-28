package com.memora.app.domain.extraction

/** Persistence boundary for durable image EXIF extraction headers. */
interface ImageExifExtractionPersistence {
    suspend fun findHeader(record: ImageExifExtractionRecord): ImageExifExtractionRecord?

    suspend fun insert(record: ImageExifExtractionRecord)

    suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int
}
