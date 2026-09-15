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

/** Schedules unique SAF PDF discovery drain work for one approved folder source. */
interface SafPdfDiscoveryWorkScheduler {
    fun enqueueDrain(sourceId: SourceId)

    fun enqueueContinuation(sourceId: SourceId)

    /**
     * Ends the drain and any continuation queued behind it. Every other drain in
     * the app has this; without it a self-chaining scan had no way out.
     */
    fun cancelDrain(sourceId: SourceId)

    fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>>
}

/**
 * Enqueues unique SAF PDF discovery drain work for one approved folder source.
 *
 * Discovery metadata only. Never opens PDF bytes or schedules extraction.
 */
@Singleton
class DefaultSafPdfDiscoveryWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) : SafPdfDiscoveryWorkScheduler {
    override fun enqueueDrain(sourceId: SourceId) {
        workManager.enqueueUniqueWork(
            uniqueWorkName(sourceId),
            ExistingWorkPolicy.REPLACE,
            pageRequest(sourceId),
        )
    }

    override fun enqueueContinuation(sourceId: SourceId) {
        workManager.enqueueUniqueWork(
            uniqueWorkName(sourceId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            pageRequest(sourceId),
        )
    }

    override fun cancelDrain(sourceId: SourceId) {
        workManager.cancelUniqueWork(uniqueWorkName(sourceId))
    }

    override fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(sourceId))

    private fun pageRequest(sourceId: SourceId) =
        OneTimeWorkRequestBuilder<SafPdfDiscoveryWorker>()
            .setInputData(workDataOf(SafPdfDiscoveryWorker.KEY_SOURCE_ID to sourceId.value))
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .addTag(TAG_SAF_PDF_DISCOVERY)
            .build()

    companion object {
        const val TAG_SAF_PDF_DISCOVERY = "saf-pdf-discovery"

        fun uniqueWorkName(sourceId: SourceId): String = "saf-pdf-discovery:${sourceId.value}"
    }
}
