package com.memora.app.data.local

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.extraction.PhotoOcrExtractionPersistence
import com.memora.app.domain.extraction.PhotoOcrExtractionRecord
import com.memora.app.domain.extraction.PhotoOcrSchemaVersion

class RoomPhotoOcrExtractionPersistencePort(
    private val database: () -> MemoraDatabase,
) : PhotoOcrExtractionPersistence {
    override suspend fun findHeader(record: PhotoOcrExtractionRecord): PhotoOcrExtractionRecord? {
        val entity = database().photoOcrExtractionDao().findHeader(
            sourceId = record.asset.identity.sourceId.value,
            sourceAssetKey = record.asset.identity.sourceAssetKey.value,
            fingerprint = record.asset.fingerprint.value,
            schemaVersion = record.schemaVersion.value,
        ) ?: return null
        return entity.toDomain(record.asset)
    }

    override suspend fun insert(record: PhotoOcrExtractionRecord) {
        database().photoOcrExtractionDao().insert(
            PhotoOcrExtractionEntity(
                sourceId = record.asset.identity.sourceId.value,
                sourceAssetKey = record.asset.identity.sourceAssetKey.value,
                fingerprint = record.asset.fingerprint.value,
                schemaVersion = record.schemaVersion.value,
                fullText = record.fullText,
                textTruncated = record.textTruncated,
                engineId = record.engineId,
                engineVersion = record.engineVersion,
                extractedAtEpochMillis = record.extractedAtEpochMillis,
                createdAtEpochMillis = System.currentTimeMillis(),
                integrity = record.integrity,
            ),
        )
    }

    override suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int =
        database().photoOcrExtractionDao().countCurrentForSource(sourceId, schemaVersion)
}

private fun PhotoOcrExtractionEntity.toDomain(asset: Asset): PhotoOcrExtractionRecord =
    PhotoOcrExtractionRecord(
        asset = asset,
        schemaVersion = PhotoOcrSchemaVersion(schemaVersion),
        fullText = fullText,
        textTruncated = textTruncated,
        engineId = engineId,
        engineVersion = engineVersion,
        extractedAtEpochMillis = extractedAtEpochMillis,
        integrity = integrity,
    )
