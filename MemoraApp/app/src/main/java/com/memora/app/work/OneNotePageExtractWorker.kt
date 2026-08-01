package com.memora.app.work

import android.content.Context
import android.os.CancellationSignal
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.memora.app.application.notes.PendingOneNotePageExtractor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs one bounded pending OneNote page text extract.
 * Needs network for Graph HTML; stores plain text only on-device.
 */
@HiltWorker
class OneNotePageExtractWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runPendingExtract: PendingOneNotePageExtractor,
    private val scheduler: OneNotePageExtractWorkScheduler,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val afterKey = inputData.getString(KEY_AFTER_SOURCE_ASSET_KEY)?.takeIf { it.isNotBlank() }
        val cancellationSignal = CancellationSignal()
        if (isStopped) {
            cancellationSignal.cancel()
            return Result.retry()
        }

        val outcome = try {
            runPendingExtract(afterKey, cancellationSignal)
        } catch (_: Exception) {
            return Result.retry()
        }

        return when (val decision = OneNotePageExtractWorkDecisionMapper.map(outcome)) {
            is OneNotePageExtractWorkDecision.Continue -> {
                scheduler.enqueueContinuation(decision.afterSourceAssetKey)
                Result.success(
                    workDataOf(
                        KEY_HAS_MORE to true,
                        KEY_AFTER_SOURCE_ASSET_KEY to decision.afterSourceAssetKey,
                    ),
                )
            }

            OneNotePageExtractWorkDecision.CompletedDrain -> Result.success(
                workDataOf(KEY_HAS_MORE to false),
            )

            OneNotePageExtractWorkDecision.AccessStopped -> Result.failure(
                workDataOf(KEY_FAILURE_REASON to REASON_ACCESS_STOPPED),
            )

            OneNotePageExtractWorkDecision.RetryableFailure -> Result.retry()
        }
    }

    companion object {
        const val KEY_AFTER_SOURCE_ASSET_KEY = "after_source_asset_key"
        const val KEY_HAS_MORE = "has_more"
        const val KEY_FAILURE_REASON = "failure_reason"
        const val REASON_ACCESS_STOPPED = "access_stopped"
    }
}
