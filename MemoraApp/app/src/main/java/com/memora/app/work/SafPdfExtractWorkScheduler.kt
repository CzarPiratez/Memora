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

/** Schedules unique SAF PDF extract drain work for one approved folder source. */
interface SafPdfExtractWorkScheduler {
    fun enqueueDrain(sourceId: SourceId)

    fun enqueueContinuation(sourceId: SourceId, afterSourceAssetKey: String)

    fun cancel(sourceId: SourceId)

    fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>>
}

/**
 * Enqueues unique SAF PDF extract drain work for one approved folder source.
 *
 * One pending PDF per work unit via broker + isolated parser. Never uses network/AI.
 */
@Singleton
class DefaultSafPdfExtractWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) : SafPdfExtractWorkScheduler {
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
        OneTimeWorkRequestBuilder<SafPdfExtractWorker>()
            .setInputData(
                workDataOf(
                    SafPdfExtractWorker.KEY_SOURCE_ID to sourceId.value,
                    SafPdfExtractWorker.KEY_AFTER_SOURCE_ASSET_KEY to afterSourceAssetKey.orEmpty(),
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .addTag(TAG_SAF_PDF_EXTRACT)
            .build()

    companion object {
        const val TAG_SAF_PDF_EXTRACT = "saf-pdf-extract"

        fun uniqueWorkName(sourceId: SourceId): String = "saf-pdf-extract:${sourceId.value}"
    }
}
