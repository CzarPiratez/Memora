package com.memora.app.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.memora.app.domain.asset.SourceId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Schedules unique MediaStore screenshot OCR extract drain work. */
interface MediaStoreScreenshotOcrExtractWorkScheduler {
    fun enqueueDrain(sourceId: SourceId)

    fun enqueueContinuation(sourceId: SourceId, afterSourceAssetKey: String)

    fun cancel(sourceId: SourceId)

    fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>>
}

@Singleton
class DefaultMediaStoreScreenshotOcrExtractWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) : MediaStoreScreenshotOcrExtractWorkScheduler {
    override fun enqueueDrain(sourceId: SourceId) {
        workManager.enqueueUniqueWork(
            uniqueWorkName(sourceId),
            ExistingWorkPolicy.REPLACE,
            pageRequest(sourceId, afterSourceAssetKey = null),
        )
    }

    override fun enqueueContinuation(sourceId: SourceId, afterSourceAssetKey: String) {
        workManager.enqueueUniqueWork(
            uniqueWorkName(sourceId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            pageRequest(sourceId, afterSourceAssetKey = afterSourceAssetKey),
        )
    }

    override fun cancel(sourceId: SourceId) {
        workManager.cancelUniqueWork(uniqueWorkName(sourceId))
    }

    override fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(sourceId))

    private fun pageRequest(sourceId: SourceId, afterSourceAssetKey: String?) =
        OneTimeWorkRequestBuilder<MediaStoreScreenshotOcrExtractWorker>()
            .setInputData(
                workDataOf(
                    MediaStoreScreenshotOcrExtractWorker.KEY_SOURCE_ID to sourceId.value,
                    MediaStoreScreenshotOcrExtractWorker.KEY_AFTER_SOURCE_ASSET_KEY to
                        afterSourceAssetKey.orEmpty(),
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .build(),
            )
            .addTag(TAG_MEDIASTORE_SCREENSHOT_OCR_EXTRACT)
            .build()

    companion object {
        const val TAG_MEDIASTORE_SCREENSHOT_OCR_EXTRACT = "mediastore-screenshot-ocr-extract"

        fun uniqueWorkName(sourceId: SourceId): String =
            "mediastore-screenshot-ocr-extract:${sourceId.value}"
    }
}
