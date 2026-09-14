package com.memora.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.memora.app.application.intelligence.MeaningIndexDrainSession
import com.memora.app.application.intelligence.MeaningIndexDrainSessionResult
import com.memora.app.application.intelligence.RunPendingMeaningIndex
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Bounded meaning-index drain. Calls only [RunPendingMeaningIndex], looping
 * batches until the queue is empty or the wall-clock budget expires, then
 * re-enqueues while more remain.
 */
@HiltWorker
class MeaningIndexWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runPendingMeaningIndex: RunPendingMeaningIndex,
    private val scheduler: MeaningIndexWorkScheduler,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (isStopped) {
            return Result.success(outputOf(stopped = true))
        }
        val session = MeaningIndexDrainSession(
            runPending = { nowEpochMs -> runPendingMeaningIndex(nowEpochMs = nowEpochMs) },
            nowMs = { System.currentTimeMillis() },
        )
        val outcome = session.run(
            isStopped = { isStopped },
            onProgress = { progress ->
                setProgress(
                    workDataOf(
                        KEY_INDEXED to progress.indexedMemories,
                        KEY_REMAINING to progress.remainingPending,
                        KEY_HAS_MORE to (progress.remainingPending > 0),
                    ),
                )
            },
        )
        if (isStopped) {
            return Result.success(
                outputOf(
                    indexed = outcome.indexedMemories,
                    remaining = outcome.remainingPending,
                    hasMore = outcome.remainingPending > 0,
                    stopped = true,
                ),
            )
        }
        return when (MeaningIndexWorkDecisionMapper.map(outcome)) {
            MeaningIndexWorkDecision.Continue -> {
                scheduler.enqueueContinuation()
                Result.success(
                    outputOf(
                        indexed = outcome.indexedMemories,
                        remaining = outcome.remainingPending,
                        hasMore = true,
                    ),
                )
            }
            MeaningIndexWorkDecision.CompletedDrain ->
                Result.success(
                    outputOf(
                        indexed = outcome.indexedMemories,
                        remaining = outcome.remainingPending,
                        hasMore = false,
                    ),
                )
            MeaningIndexWorkDecision.StopAndReport ->
                Result.success(stopAndReportOutput(outcome))
            MeaningIndexWorkDecision.RetryableFailure -> Result.retry()
        }
    }

    private fun stopAndReportOutput(outcome: MeaningIndexDrainSessionResult) = when (outcome) {
        is MeaningIndexDrainSessionResult.EngineUnavailable -> outputOf(
            indexed = outcome.indexedMemories,
            remaining = outcome.remainingPending,
            hasMore = false,
            engineUnavailable = true,
        )
        is MeaningIndexDrainSessionResult.SelectionDisagreed -> outputOf(
            indexed = outcome.indexedMemories,
            remaining = outcome.pendingCount,
            hasMore = false,
            disagreedPending = outcome.pendingCount,
        )
        is MeaningIndexDrainSessionResult.Stopped -> outputOf(
            indexed = outcome.indexedMemories,
            remaining = outcome.remainingPending,
            hasMore = outcome.remainingPending > 0,
            stopped = true,
        )
        else -> outputOf(
            indexed = outcome.indexedMemories,
            remaining = outcome.remainingPending,
            hasMore = false,
        )
    }

    private fun outputOf(
        indexed: Int = 0,
        remaining: Int = 0,
        hasMore: Boolean = false,
        stopped: Boolean = false,
        engineUnavailable: Boolean = false,
        disagreedPending: Int = 0,
    ) = workDataOf(
        KEY_INDEXED to indexed,
        KEY_REMAINING to remaining,
        KEY_HAS_MORE to hasMore,
        KEY_STOPPED to stopped,
        KEY_ENGINE_UNAVAILABLE to engineUnavailable,
        KEY_DISAGREED_PENDING to disagreedPending,
    )

    companion object {
        const val KEY_INDEXED = "indexed_memories"
        const val KEY_REMAINING = "remaining_pending"
        const val KEY_HAS_MORE = "has_more"
        const val KEY_STOPPED = "stopped"
        const val KEY_ENGINE_UNAVAILABLE = "engine_unavailable"
        const val KEY_DISAGREED_PENDING = "disagreed_pending"
    }
}
