package com.memora.app.data.local

import androidx.room.withTransaction
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
        val db = database()
        val sourceId = record.asset.identity.sourceId.value
        val sourceAssetKey = record.asset.identity.sourceAssetKey.value
        val fingerprint = record.asset.fingerprint.value
        val schemaVersion = record.schemaVersion.value

        val derivationId = com.memora.app.domain.indexing.IndexStageState.computeDerivationId(
            stage = com.memora.app.domain.indexing.IndexStage.OCR,
            schemaVersion = schemaVersion,
            engineVersion = record.engineVersion,
        )

        db.withTransaction {
            // Atomic CAS check INSIDE transaction: Verify current asset fingerprint matches Worker's expected V1 fingerprint via EXCLUSIVE row lock
            val matchCount = db.assetDao().verifyAndLockFingerprint(sourceId, sourceAssetKey, fingerprint)
            if (matchCount == 0) {
                // Stale worker: Asset was modified to a newer fingerprint while worker was running. Abort transaction.
                return@withTransaction
            }

            val existingState = db.indexStageStateDao().find(
                sourceId = sourceId,
                sourceAssetKey = sourceAssetKey,
                fingerprint = fingerprint,
                stage = com.memora.app.domain.indexing.IndexStage.OCR.name,
                derivationId = derivationId,
            )
            val newAttemptCount = (existingState?.attemptCount ?: 0) + 1

            db.photoOcrExtractionDao().insert(
                PhotoOcrExtractionEntity(
                    sourceId = sourceId,
                    sourceAssetKey = sourceAssetKey,
                    fingerprint = fingerprint,
                    schemaVersion = schemaVersion,
                    fullText = record.fullText,
                    textTruncated = record.textTruncated,
                    engineId = record.engineId,
                    engineVersion = record.engineVersion,
                    extractedAtEpochMillis = record.extractedAtEpochMillis,
                    createdAtEpochMillis = System.currentTimeMillis(),
                    integrity = record.integrity,
                ),
            )
            db.indexStageStateDao().upsert(
                IndexStageStateEntity(
                    sourceId = sourceId,
                    sourceAssetKey = sourceAssetKey,
                    fingerprint = fingerprint,
                    stage = com.memora.app.domain.indexing.IndexStage.OCR.name,
                    derivationId = derivationId,
                    currentStatus = com.memora.app.domain.indexing.IndexStageStatus.VALID.name,
                    lastAttemptStatus = "SUCCESS",
                    lastFailureClass = null,
                    lastFailureCode = null,
                    lastFailureMessage = null,
                    attemptCount = newAttemptCount,
                    runId = null,
                    schemaVersion = schemaVersion,
                    modelId = null,
                    modelVersion = null,
                    engineVersion = record.engineVersion,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            db.clearMemoryAssemblySkips(
                sourceId = sourceId,
                sourceAssetKey = sourceAssetKey,
                fingerprint = fingerprint,
            )
        }
    }

    override suspend fun recordFailure(
        asset: Asset,
        schemaVersion: PhotoOcrSchemaVersion,
        engineVersion: String,
        failureClass: com.memora.app.domain.indexing.IndexFailureClass,
        failureCode: String,
        failureMessage: String,
    ) {
        val db = database()
        val sourceId = asset.identity.sourceId.value
        val sourceAssetKey = asset.identity.sourceAssetKey.value
        val fingerprint = asset.fingerprint.value

        val derivationId = com.memora.app.domain.indexing.IndexStageState.computeDerivationId(
            stage = com.memora.app.domain.indexing.IndexStage.OCR,
            schemaVersion = schemaVersion.value,
            engineVersion = engineVersion,
        )

        db.withTransaction {
            val matchCount = db.assetDao().verifyAndLockFingerprint(sourceId, sourceAssetKey, fingerprint)
            if (matchCount == 0) {
                return@withTransaction
            }

            val existingState = db.indexStageStateDao().find(
                sourceId = sourceId,
                sourceAssetKey = sourceAssetKey,
                fingerprint = fingerprint,
                stage = com.memora.app.domain.indexing.IndexStage.OCR.name,
                derivationId = derivationId,
            )

            // CRITICAL INVARIANT: If pre-existing output is VALID, currentStatus REMAINS VALID!
            // If no valid output existed, currentStatus becomes FAILED.
            val newCurrentStatus = if (existingState?.currentStatus == com.memora.app.domain.indexing.IndexStageStatus.VALID.name) {
                com.memora.app.domain.indexing.IndexStageStatus.VALID.name
            } else {
                com.memora.app.domain.indexing.IndexStageStatus.FAILED.name
            }

            val newAttemptCount = (existingState?.attemptCount ?: 0) + 1

            db.indexStageStateDao().upsert(
                IndexStageStateEntity(
                    sourceId = sourceId,
                    sourceAssetKey = sourceAssetKey,
                    fingerprint = fingerprint,
                    stage = com.memora.app.domain.indexing.IndexStage.OCR.name,
                    derivationId = derivationId,
                    currentStatus = newCurrentStatus,
                    lastAttemptStatus = "FAILED",
                    lastFailureClass = failureClass.name,
                    lastFailureCode = failureCode,
                    lastFailureMessage = failureMessage,
                    attemptCount = newAttemptCount,
                    runId = null,
                    schemaVersion = schemaVersion.value,
                    modelId = null,
                    modelVersion = null,
                    engineVersion = engineVersion,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
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
