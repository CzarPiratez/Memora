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

interface MediaStorePhotoOcrExtractWorkScheduler {
    fun enqueueDrain(sourceId: SourceId)
    fun enqueueContinuation(sourceId: SourceId, afterSourceAssetKey: String)
    fun cancel(sourceId: SourceId)
    fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>>
}

@Singleton
class DefaultMediaStorePhotoOcrExtractWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) : MediaStorePhotoOcrExtractWorkScheduler {
    override fun enqueueDrain(sourceId: SourceId) {
        workManager.enqueueUniqueWork(
            uniqueWorkName(sourceId),
            ExistingWorkPolicy.REPLACE,
            request(sourceId, null),
        )
    }

    override fun enqueueContinuation(sourceId: SourceId, afterSourceAssetKey: String) {
        workManager.enqueueUniqueWork(
            uniqueWorkName(sourceId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request(sourceId, afterSourceAssetKey),
        )
    }

    override fun cancel(sourceId: SourceId) {
        workManager.cancelUniqueWork(uniqueWorkName(sourceId))
    }

    override fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(sourceId))

    private fun request(sourceId: SourceId, afterSourceAssetKey: String?) =
        OneTimeWorkRequestBuilder<MediaStorePhotoOcrExtractWorker>()
            .setInputData(
                workDataOf(
                    MediaStorePhotoOcrExtractWorker.KEY_SOURCE_ID to sourceId.value,
                    MediaStorePhotoOcrExtractWorker.KEY_AFTER_SOURCE_ASSET_KEY to
                        afterSourceAssetKey.orEmpty(),
                ),
            )
            .setConstraints(Constraints.Builder().build())
            .addTag(TAG_MEDIASTORE_PHOTO_OCR_EXTRACT)
            .build()

    companion object {
        const val TAG_MEDIASTORE_PHOTO_OCR_EXTRACT = "mediastore-photo-ocr-extract"

        fun uniqueWorkName(sourceId: SourceId) =
            "mediastore-photo-ocr-extract:${sourceId.value}"
    }
}
