package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.indexing.IndexFailureClass

interface PhotoOcrExtractionPersistence {
    suspend fun findHeader(record: PhotoOcrExtractionRecord): PhotoOcrExtractionRecord?

    suspend fun insert(record: PhotoOcrExtractionRecord)

    suspend fun recordFailure(
        asset: Asset,
        schemaVersion: PhotoOcrSchemaVersion,
        engineVersion: String,
        failureClass: IndexFailureClass,
        failureCode: String,
        failureMessage: String,
    )

    suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int
}
