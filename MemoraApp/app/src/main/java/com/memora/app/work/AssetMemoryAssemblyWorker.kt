package com.memora.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.memora.app.application.memory.AssetMemoryDrainResult
import com.memora.app.application.memory.RunPendingAssetMemoryAssembly
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One bounded Asset Memory assembly batch. Re-enqueues while [AssetMemoryDrainResult.Completed.hasMore].
 * Does not assemble on a second path — only [RunPendingAssetMemoryAssembly].
 */
@HiltWorker
class AssetMemoryAssemblyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runPendingAssembly: RunPendingAssetMemoryAssembly,
    private val scheduler: AssetMemoryAssemblyWorkScheduler,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (isStopped) {
            return Result.success(workDataOf(KEY_STOPPED to true))
        }
        val result = try {
            runPendingAssembly()
        } catch (_: Exception) {
            return Result.retry()
        }
        if (isStopped) {
            return Result.success(outputOf(result, stopped = true, hasMore = false))
        }
        return when (AssetMemoryAssemblyWorkDecisionMapper.map(result)) {
            AssetMemoryAssemblyWorkDecision.Continue -> {
                if (isStopped) {
                    return Result.success(outputOf(result, stopped = true, hasMore = false))
                }
                scheduler.enqueueContinuation()
                Result.success(outputOf(result, stopped = false, hasMore = true))
            }
            AssetMemoryAssemblyWorkDecision.CompletedDrain ->
                Result.success(outputOf(result, stopped = false, hasMore = false))
            AssetMemoryAssemblyWorkDecision.RetryableFailure -> Result.retry()
        }
    }

    private fun outputOf(
        result: AssetMemoryDrainResult,
        stopped: Boolean,
        hasMore: Boolean,
    ) = when (result) {
        is AssetMemoryDrainResult.Completed -> workDataOf(
            KEY_ASSEMBLED to result.assembledCount,
            KEY_SKIPPED to result.skippedCount,
            KEY_READY_COUNT to result.currentReadyCount,
            KEY_HAS_MORE to hasMore,
            KEY_STOPPED to stopped,
        )
        is AssetMemoryDrainResult.FailedSafely -> workDataOf(
            KEY_ASSEMBLED to result.assembledCount,
            KEY_SKIPPED to result.skippedCount,
            KEY_READY_COUNT to result.currentReadyCount,
            KEY_HAS_MORE to false,
            KEY_STOPPED to stopped,
        )
    }

    companion object {
        const val KEY_ASSEMBLED = "assembled_count"
        const val KEY_SKIPPED = "skipped_count"
        const val KEY_READY_COUNT = "ready_count"
        const val KEY_HAS_MORE = "has_more"
        const val KEY_STOPPED = "stopped"
    }
}
