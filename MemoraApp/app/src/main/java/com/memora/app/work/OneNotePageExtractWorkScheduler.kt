package com.memora.app.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Schedules unique OneNote page text extract drain work (needs network). */
interface OneNotePageExtractWorkScheduler {
    fun enqueueDrain()

    fun enqueueContinuation(afterSourceAssetKey: String)

    fun cancel()

    fun observeUniqueWork(): Flow<List<WorkInfo>>
}

@Singleton
class DefaultOneNotePageExtractWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) : OneNotePageExtractWorkScheduler {
    override fun enqueueDrain() {
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            pageRequest(afterSourceAssetKey = null),
        )
    }

    override fun enqueueContinuation(afterSourceAssetKey: String) {
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            pageRequest(afterSourceAssetKey = afterSourceAssetKey),
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    override fun observeUniqueWork(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)

    private fun pageRequest(afterSourceAssetKey: String?) =
        OneTimeWorkRequestBuilder<OneNotePageExtractWorker>()
            .setInputData(
                workDataOf(
                    OneNotePageExtractWorker.KEY_AFTER_SOURCE_ASSET_KEY to
                        afterSourceAssetKey.orEmpty(),
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .addTag(TAG_ONENOTE_PAGE_EXTRACT)
            .build()

    companion object {
        const val TAG_ONENOTE_PAGE_EXTRACT = "onenote-page-extract"
        const val UNIQUE_WORK_NAME = "onenote-page-extract"
    }
}
