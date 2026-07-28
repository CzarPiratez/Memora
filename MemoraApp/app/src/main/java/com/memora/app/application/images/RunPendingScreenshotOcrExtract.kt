package com.memora.app.application.images

import android.content.Context
import android.os.CancellationSignal
import com.memora.app.data.local.RoomScreenshotOcrExtractionPersistencePort
import com.memora.app.data.mediastore.MediaStoreAccess
import com.memora.app.data.mediastore.ScreenshotOcrReader
import com.memora.app.data.mediastore.mediaStoreImageAccess
import com.memora.app.data.security.MemoraDatabaseHandle
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ScreenshotOcrExtractionRecord
import com.memora.app.domain.extraction.ScreenshotOcrReadResult
import com.memora.app.domain.extraction.ScreenshotOcrSchemaVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs one bounded OCR extract for the next pending SCREENSHOT. */
interface PendingScreenshotOcrExtractor {
    suspend operator fun invoke(
        sourceId: SourceId,
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingScreenshotOcrExtractOutcome
}

sealed interface PendingScreenshotOcrExtractOutcome {
    data object NoPending : PendingScreenshotOcrExtractOutcome

    data class Persisted(
        val processedSourceAssetKey: String,
        val hasMorePending: Boolean,
    ) : PendingScreenshotOcrExtractOutcome {
        init {
            require(processedSourceAssetKey.isNotBlank())
        }
    }

    data object AccessStopped : PendingScreenshotOcrExtractOutcome

    data object RetryableFailure : PendingScreenshotOcrExtractOutcome

    data object Cancelled : PendingScreenshotOcrExtractOutcome
}

@Singleton
class RunPendingScreenshotOcrExtract @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
    private val screenshotOcrReader: ScreenshotOcrReader,
    databaseHandle: MemoraDatabaseHandle,
) : PendingScreenshotOcrExtractor {
    private val persistence = RoomScreenshotOcrExtractionPersistencePort { databaseHandle.database() }

    override suspend operator fun invoke(
        sourceId: SourceId,
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingScreenshotOcrExtractOutcome = withContext(Dispatchers.IO) {
        if (cancellationSignal.isCanceled) {
            return@withContext PendingScreenshotOcrExtractOutcome.Cancelled
        }
        if (mediaStoreImageAccess(context) == MediaStoreAccess.REQUIRED) {
            return@withContext PendingScreenshotOcrExtractOutcome.AccessStopped
        }

        val asset = assetRepository.findNextScreenshotPendingOcrExtract(
            sourceId = sourceId,
            schemaVersion = SCHEMA.value,
            afterSourceAssetKey = afterSourceAssetKey,
        ) ?: return@withContext PendingScreenshotOcrExtractOutcome.NoPending

        val processedKey = asset.identity.sourceAssetKey.value
        if (cancellationSignal.isCanceled) {
            return@withContext PendingScreenshotOcrExtractOutcome.Cancelled
        }

        when (val read = screenshotOcrReader.read(asset)) {
            ScreenshotOcrReadResult.AccessStopped -> PendingScreenshotOcrExtractOutcome.AccessStopped
            ScreenshotOcrReadResult.RetryableFailure -> PendingScreenshotOcrExtractOutcome.RetryableFailure
            is ScreenshotOcrReadResult.Text -> {
                val record = ScreenshotOcrExtractionRecord(
                    asset = asset,
                    schemaVersion = SCHEMA,
                    fullText = read.fullText,
                    textTruncated = read.textTruncated,
                    engineId = read.engineId,
                    engineVersion = read.engineVersion,
                    extractedAtEpochMillis = System.currentTimeMillis(),
                )
                persistence.insert(record)
                val hasMore = assetRepository.findNextScreenshotPendingOcrExtract(
                    sourceId = sourceId,
                    schemaVersion = SCHEMA.value,
                    afterSourceAssetKey = processedKey,
                ) != null
                PendingScreenshotOcrExtractOutcome.Persisted(
                    processedSourceAssetKey = processedKey,
                    hasMorePending = hasMore,
                )
            }
        }
    }

    companion object {
        val SCHEMA: ScreenshotOcrSchemaVersion = ScreenshotOcrSchemaVersion.V1
    }
}
