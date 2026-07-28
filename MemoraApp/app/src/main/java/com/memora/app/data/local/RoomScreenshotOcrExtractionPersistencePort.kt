package com.memora.app.data.local

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.extraction.ScreenshotOcrExtractionPersistence
import com.memora.app.domain.extraction.ScreenshotOcrExtractionRecord
import com.memora.app.domain.extraction.ScreenshotOcrSchemaVersion

class RoomScreenshotOcrExtractionPersistencePort(
    private val database: () -> MemoraDatabase,
) : ScreenshotOcrExtractionPersistence {
    override suspend fun findHeader(record: ScreenshotOcrExtractionRecord): ScreenshotOcrExtractionRecord? {
        val entity = database().screenshotOcrExtractionDao().findHeader(
            sourceId = record.asset.identity.sourceId.value,
            sourceAssetKey = record.asset.identity.sourceAssetKey.value,
            fingerprint = record.asset.fingerprint.value,
            schemaVersion = record.schemaVersion.value,
        ) ?: return null
        return entity.toDomain(record.asset)
    }

    override suspend fun insert(record: ScreenshotOcrExtractionRecord) {
        val now = System.currentTimeMillis()
        database().screenshotOcrExtractionDao().insert(
            ScreenshotOcrExtractionEntity(
                sourceId = record.asset.identity.sourceId.value,
                sourceAssetKey = record.asset.identity.sourceAssetKey.value,
                fingerprint = record.asset.fingerprint.value,
                schemaVersion = record.schemaVersion.value,
                fullText = record.fullText,
                textTruncated = record.textTruncated,
                engineId = record.engineId,
                engineVersion = record.engineVersion,
                extractedAtEpochMillis = record.extractedAtEpochMillis,
                createdAtEpochMillis = now,
                integrity = record.integrity,
            ),
        )
    }

    override suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int =
        database().screenshotOcrExtractionDao().countCurrentForSource(sourceId, schemaVersion)
}

private fun ScreenshotOcrExtractionEntity.toDomain(asset: Asset): ScreenshotOcrExtractionRecord =
    ScreenshotOcrExtractionRecord(
        asset = asset,
        schemaVersion = ScreenshotOcrSchemaVersion(schemaVersion),
        fullText = fullText,
        textTruncated = textTruncated,
        engineId = engineId,
        engineVersion = engineVersion,
        extractedAtEpochMillis = extractedAtEpochMillis,
        integrity = integrity,
    )
