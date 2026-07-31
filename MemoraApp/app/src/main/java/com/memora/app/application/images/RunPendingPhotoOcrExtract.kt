package com.memora.app.application.images

import android.content.Context
import android.os.CancellationSignal
import com.memora.app.data.local.RoomPhotoOcrExtractionPersistencePort
import com.memora.app.data.mediastore.MediaStoreAccess
import com.memora.app.data.mediastore.PhotoOcrReader
import com.memora.app.data.mediastore.mediaStoreImageAccess
import com.memora.app.data.security.MemoraDatabaseHandle
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.PhotoOcrExtractionRecord
import com.memora.app.domain.extraction.PhotoOcrReadResult
import com.memora.app.domain.extraction.PhotoOcrSchemaVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PendingPhotoOcrExtractor {
    suspend operator fun invoke(
        sourceId: SourceId,
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingPhotoOcrExtractOutcome
}

sealed interface PendingPhotoOcrExtractOutcome {
    data object NoPending : PendingPhotoOcrExtractOutcome
    data class Persisted(
        val processedSourceAssetKey: String,
        val hasMorePending: Boolean,
    ) : PendingPhotoOcrExtractOutcome {
        init {
            require(processedSourceAssetKey.isNotBlank())
        }
    }
    data object AccessStopped : PendingPhotoOcrExtractOutcome
    data object RetryableFailure : PendingPhotoOcrExtractOutcome
    data object Cancelled : PendingPhotoOcrExtractOutcome
}

@Singleton
class RunPendingPhotoOcrExtract @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
    private val photoOcrReader: PhotoOcrReader,
    databaseHandle: MemoraDatabaseHandle,
) : PendingPhotoOcrExtractor {
    private val persistence = RoomPhotoOcrExtractionPersistencePort { databaseHandle.database() }

    override suspend fun invoke(
        sourceId: SourceId,
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingPhotoOcrExtractOutcome = withContext(Dispatchers.IO) {
        if (cancellationSignal.isCanceled) return@withContext PendingPhotoOcrExtractOutcome.Cancelled
        if (mediaStoreImageAccess(context) == MediaStoreAccess.REQUIRED) {
            return@withContext PendingPhotoOcrExtractOutcome.AccessStopped
        }
        val asset = assetRepository.findNextPhotoPendingOcrExtract(
            sourceId,
            SCHEMA.value,
            afterSourceAssetKey,
        ) ?: return@withContext PendingPhotoOcrExtractOutcome.NoPending
        val processedKey = asset.identity.sourceAssetKey.value
        if (cancellationSignal.isCanceled) return@withContext PendingPhotoOcrExtractOutcome.Cancelled

        when (val read = photoOcrReader.read(asset)) {
            PhotoOcrReadResult.AccessStopped -> PendingPhotoOcrExtractOutcome.AccessStopped
            PhotoOcrReadResult.RetryableFailure -> PendingPhotoOcrExtractOutcome.RetryableFailure
            is PhotoOcrReadResult.Text -> {
                persistence.insert(
                    PhotoOcrExtractionRecord(
                        asset = asset,
                        schemaVersion = SCHEMA,
                        fullText = read.fullText,
                        textTruncated = read.textTruncated,
                        engineId = read.engineId,
                        engineVersion = read.engineVersion,
                        extractedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
                PendingPhotoOcrExtractOutcome.Persisted(
                    processedSourceAssetKey = processedKey,
                    hasMorePending = assetRepository.findNextPhotoPendingOcrExtract(
                        sourceId,
                        SCHEMA.value,
                        processedKey,
                    ) != null,
                )
            }
        }
    }

    companion object {
        val SCHEMA = PhotoOcrSchemaVersion.V1
    }
}
