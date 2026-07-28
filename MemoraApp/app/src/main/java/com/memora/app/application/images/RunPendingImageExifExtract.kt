package com.memora.app.application.images

import android.os.CancellationSignal
import com.memora.app.data.local.RoomImageExifExtractionPersistencePort
import com.memora.app.data.mediastore.ImageExifReader
import com.memora.app.data.mediastore.MediaStoreAccess
import com.memora.app.data.mediastore.mediaStoreImageAccess
import com.memora.app.data.security.MemoraDatabaseHandle
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ImageExifExtractionRecord
import com.memora.app.domain.extraction.ImageExifReadResult
import com.memora.app.domain.extraction.ImageExifSchemaVersion
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs one bounded EXIF extract for the next pending PHOTO/SCREENSHOT. */
interface PendingImageExifExtractor {
    suspend operator fun invoke(
        sourceId: SourceId,
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingImageExifExtractOutcome
}

sealed interface PendingImageExifExtractOutcome {
    data object NoPending : PendingImageExifExtractOutcome

    data class Persisted(
        val processedSourceAssetKey: String,
        val hasMorePending: Boolean,
    ) : PendingImageExifExtractOutcome {
        init {
            require(processedSourceAssetKey.isNotBlank())
        }
    }

    data object AccessStopped : PendingImageExifExtractOutcome

    data object RetryableFailure : PendingImageExifExtractOutcome

    data object Cancelled : PendingImageExifExtractOutcome
}

@Singleton
class RunPendingImageExifExtract @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
    private val imageExifReader: ImageExifReader,
    databaseHandle: MemoraDatabaseHandle,
) : PendingImageExifExtractor {
    private val persistence = RoomImageExifExtractionPersistencePort { databaseHandle.database() }

    override suspend operator fun invoke(
        sourceId: SourceId,
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingImageExifExtractOutcome = withContext(Dispatchers.IO) {
        if (cancellationSignal.isCanceled) {
            return@withContext PendingImageExifExtractOutcome.Cancelled
        }
        if (mediaStoreImageAccess(context) == MediaStoreAccess.REQUIRED) {
            return@withContext PendingImageExifExtractOutcome.AccessStopped
        }

        val asset = assetRepository.findNextImagePendingExifExtract(
            sourceId = sourceId,
            schemaVersion = SCHEMA.value,
            afterSourceAssetKey = afterSourceAssetKey,
        ) ?: return@withContext PendingImageExifExtractOutcome.NoPending

        val processedKey = asset.identity.sourceAssetKey.value
        if (cancellationSignal.isCanceled) {
            return@withContext PendingImageExifExtractOutcome.Cancelled
        }

        when (val read = imageExifReader.read(asset)) {
            ImageExifReadResult.AccessStopped -> PendingImageExifExtractOutcome.AccessStopped
            ImageExifReadResult.RetryableFailure -> PendingImageExifExtractOutcome.RetryableFailure
            is ImageExifReadResult.Facts -> {
                val record = ImageExifExtractionRecord(
                    asset = asset,
                    schemaVersion = SCHEMA,
                    assetKind = asset.type,
                    datetimeOriginal = read.datetimeOriginal,
                    imageWidth = read.imageWidth,
                    imageHeight = read.imageHeight,
                    orientation = read.orientation,
                    make = read.make,
                    model = read.model,
                    extractedAtEpochMillis = System.currentTimeMillis(),
                )
                persistence.insert(record)
                val hasMore = assetRepository.findNextImagePendingExifExtract(
                    sourceId = sourceId,
                    schemaVersion = SCHEMA.value,
                    afterSourceAssetKey = processedKey,
                ) != null
                PendingImageExifExtractOutcome.Persisted(
                    processedSourceAssetKey = processedKey,
                    hasMorePending = hasMore,
                )
            }
        }
    }

    companion object {
        val SCHEMA: ImageExifSchemaVersion = ImageExifSchemaVersion.V1
    }
}
