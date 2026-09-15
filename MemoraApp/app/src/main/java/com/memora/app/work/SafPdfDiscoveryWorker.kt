package com.memora.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.memora.app.application.documents.SafPdfFolderIndexer
import com.memora.app.domain.asset.SourceId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs one bounded SAF PDF metadata discovery page for an approved folder.
 *
 * Never opens PDF bytes, binds the isolated parser, or writes extraction text.
 */
@HiltWorker
class SafPdfDiscoveryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val indexPdfFolder: SafPdfFolderIndexer,
    private val scheduler: SafPdfDiscoveryWorkScheduler,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (isStopped) return stoppedResult()

        val sourceIdValue = inputData.getString(KEY_SOURCE_ID)?.takeIf { it.isNotBlank() }
            ?: return Result.failure(workDataOf(KEY_FAILURE_REASON to REASON_INVALID_INPUT))

        val sourceId = try {
            SourceId(sourceIdValue)
        } catch (_: IllegalArgumentException) {
            return Result.failure(workDataOf(KEY_FAILURE_REASON to REASON_INVALID_INPUT))
        }

        val outcome = try {
            indexPdfFolder(sourceId)
        } catch (_: Exception) {
            return retryOrGiveUp()
        }

        if (isStopped) return stoppedResult()

        return when (val decision = SafPdfDiscoveryWorkDecisionMapper.map(outcome)) {
            is SafPdfDiscoveryWorkDecision.Continue -> {
                // Chaining the next page is what makes this drain self-driving,
                // so it is also the thing that must not survive a Stop.
                scheduler.enqueueContinuation(sourceId)
                Result.success(
                    workDataOf(
                        KEY_PAGE_ASSET_COUNT to decision.pageAssetCount,
                        KEY_HAS_MORE to true,
                    ),
                )
            }

            is SafPdfDiscoveryWorkDecision.Completed -> Result.success(
                workDataOf(
                    KEY_PAGE_ASSET_COUNT to decision.pageAssetCount,
                    KEY_HAS_MORE to false,
                ),
            )

            SafPdfDiscoveryWorkDecision.AccessStopped -> Result.failure(
                workDataOf(KEY_FAILURE_REASON to REASON_ACCESS_STOPPED),
            )

            is SafPdfDiscoveryWorkDecision.RetryableFailure -> retryOrGiveUp()
        }
    }

    /**
     * An unbounded `Result.retry()` is indistinguishable from progress: the work
     * returns to ENQUEUED on backoff and the screen keeps spinning with nothing
     * to show for it. Give up after [MAX_RUN_ATTEMPTS] so a scan that cannot
     * succeed ends in something a person can read and act on.
     */
    private fun retryOrGiveUp(): Result = if (runAttemptCount >= MAX_RUN_ATTEMPTS) {
        Result.failure(workDataOf(KEY_FAILURE_REASON to REASON_GAVE_UP))
    } else {
        Result.retry()
    }

    private fun stoppedResult(): Result =
        Result.failure(workDataOf(KEY_FAILURE_REASON to REASON_STOPPED))

    companion object {
        const val KEY_SOURCE_ID = "source_id"
        const val KEY_PAGE_ASSET_COUNT = "page_asset_count"
        const val KEY_HAS_MORE = "has_more"
        const val KEY_FAILURE_REASON = "failure_reason"
        const val REASON_ACCESS_STOPPED = "access_stopped"
        const val REASON_INVALID_INPUT = "invalid_input"
        const val REASON_GAVE_UP = "gave_up"
        const val REASON_STOPPED = "stopped"

        /** Attempts, not retries: `runAttemptCount` is 0 on the first run. */
        const val MAX_RUN_ATTEMPTS = 4
    }
}
