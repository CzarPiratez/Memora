package com.memora.app.work

import android.content.Context
import android.os.CancellationSignal
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.memora.app.application.documents.PendingPdfLocalReader
import com.memora.app.application.documents.PendingPdfLocalReadingOutcome
import com.memora.app.domain.asset.SourceId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs one bounded pending-PDF local reading unit for an approved folder.
 *
 * Opens only through the descriptor broker and isolated parser; persists via the
 * existing validated path. Never parses PDF bytes in this process.
 */
@HiltWorker
class SafPdfExtractWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runPendingReading: PendingPdfLocalReader,
    private val scheduler: SafPdfExtractWorkScheduler,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sourceIdValue = inputData.getString(KEY_SOURCE_ID)?.takeIf { it.isNotBlank() }
            ?: return Result.failure(workDataOf(KEY_FAILURE_REASON to REASON_INVALID_INPUT))

        val sourceId = try {
            SourceId(sourceIdValue)
        } catch (_: IllegalArgumentException) {
            return Result.failure(workDataOf(KEY_FAILURE_REASON to REASON_INVALID_INPUT))
        }

        val afterKey = inputData.getString(KEY_AFTER_SOURCE_ASSET_KEY)?.takeIf { it.isNotBlank() }
        val cancellationSignal = CancellationSignal()
        if (isStopped) {
            cancellationSignal.cancel()
            return Result.retry()
        }

        val outcome = try {
            runPendingReading(sourceId, afterKey, cancellationSignal)
        } catch (_: Exception) {
            return Result.retry()
        }

        return when (val decision = SafPdfExtractWorkDecisionMapper.map(outcome)) {
            is SafPdfExtractWorkDecision.Continue -> {
                scheduler.enqueueContinuation(sourceId, decision.afterSourceAssetKey)
                Result.success(
                    workDataOf(
                        KEY_HAS_MORE to true,
                        KEY_UNIT_FINISHED to true,
                        KEY_AFTER_SOURCE_ASSET_KEY to decision.afterSourceAssetKey,
                    ),
                )
            }

            SafPdfExtractWorkDecision.CompletedDrain -> Result.success(
                workDataOf(
                    KEY_HAS_MORE to false,
                    KEY_UNIT_FINISHED to (outcome !is PendingPdfLocalReadingOutcome.NoPending),
                ),
            )

            SafPdfExtractWorkDecision.AccessStopped -> Result.failure(
                workDataOf(KEY_FAILURE_REASON to REASON_ACCESS_STOPPED),
            )

            SafPdfExtractWorkDecision.RetryableFailure -> Result.retry()
        }
    }

    companion object {
        const val KEY_SOURCE_ID = "source_id"
        const val KEY_AFTER_SOURCE_ASSET_KEY = "after_source_asset_key"
        const val KEY_HAS_MORE = "has_more"
        /** True when this work unit finished one PDF (saved or password-skipped). */
        const val KEY_UNIT_FINISHED = "unit_finished"
        const val KEY_FAILURE_REASON = "failure_reason"
        const val REASON_ACCESS_STOPPED = "access_stopped"
        const val REASON_INVALID_INPUT = "invalid_input"
    }
}
