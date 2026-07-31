package com.memora.app.domain.extraction

interface PhotoOcrExtractionPersistence {
    suspend fun findHeader(record: PhotoOcrExtractionRecord): PhotoOcrExtractionRecord?

    suspend fun insert(record: PhotoOcrExtractionRecord)

    suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int
}
