package com.memora.app.application.intelligence

/**
 * One worker invocation: run bounded [RunPendingMeaningIndex] batches until
 * the queue is empty, the wall-clock budget expires, the user stops, or a
 * terminal reportable result arrives.
 *
 * Does not schedule WorkManager. The worker maps [MeaningIndexDrainSessionResult]
 * onto enqueue / success / retry.
 */
class MeaningIndexDrainSession(
    private val runPending: suspend (nowEpochMs: Long) -> RunPendingMeaningIndexResult,
    private val nowMs: () -> Long,
    private val budgetMs: Long = MeaningIndexBatchLimits.WORKER_WALL_CLOCK_BUDGET_MS,
) {
    suspend fun run(
        isStopped: () -> Boolean,
        onProgress: suspend (MeaningIndexDrainSessionProgress) -> Unit = {},
    ): MeaningIndexDrainSessionResult {
        val startedAtMs = nowMs()
        var indexedMemories = 0
        var remainingPending = 0
        var batches = 0
        while (true) {
            if (isStopped()) {
                return MeaningIndexDrainSessionResult.Stopped(
                    indexedMemories = indexedMemories,
                    remainingPending = remainingPending,
                )
            }
            if (!MeaningIndexDrainBudget.anotherBatchFits(
                    startedAtMs = startedAtMs,
                    nowMs = nowMs(),
                    batchesAlreadyRun = batches,
                    budgetMs = budgetMs,
                )
            ) {
                return MeaningIndexDrainSessionResult.BudgetExhausted(
                    indexedMemories = indexedMemories,
                    remainingPending = remainingPending,
                )
            }
            val result = try {
                runPending(nowMs())
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                return MeaningIndexDrainSessionResult.RetryableFailure
            }
            when (MeaningIndexDrainDecision.map(result)) {
                MeaningIndexDrainDecision.CompletedDrain -> {
                    if (result is RunPendingMeaningIndexResult.Completed) {
                        indexedMemories += result.memories.indexed
                        remainingPending = result.remainingPending
                    } else {
                        remainingPending = 0
                    }
                    return MeaningIndexDrainSessionResult.Finished(
                        indexedMemories = indexedMemories,
                        remainingPending = remainingPending,
                    )
                }
                MeaningIndexDrainDecision.Continue -> {
                    val completed = result as RunPendingMeaningIndexResult.Completed
                    indexedMemories += completed.memories.indexed
                    remainingPending = completed.remainingPending
                    batches += 1
                    onProgress(
                        MeaningIndexDrainSessionProgress(
                            indexedMemories = indexedMemories,
                            remainingPending = remainingPending,
                        ),
                    )
                }
                MeaningIndexDrainDecision.StopAndReport -> {
                    return when (result) {
                        is RunPendingMeaningIndexResult.EngineUnavailable ->
                            MeaningIndexDrainSessionResult.EngineUnavailable(result.reason)
                        is RunPendingMeaningIndexResult.SelectionDisagreed ->
                            MeaningIndexDrainSessionResult.SelectionDisagreed(result.pendingCount)
                        else -> MeaningIndexDrainSessionResult.RetryableFailure
                    }
                }
                MeaningIndexDrainDecision.RetryableFailure ->
                    return MeaningIndexDrainSessionResult.RetryableFailure
            }
        }
    }
}

data class MeaningIndexDrainSessionProgress(
    val indexedMemories: Int,
    val remainingPending: Int,
) {
    init {
        require(indexedMemories >= 0)
        require(remainingPending >= 0)
    }
}

sealed interface MeaningIndexDrainSessionResult {
    val indexedMemories: Int
    val remainingPending: Int

    data class Finished(
        override val indexedMemories: Int,
        override val remainingPending: Int,
    ) : MeaningIndexDrainSessionResult

    data class BudgetExhausted(
        override val indexedMemories: Int,
        override val remainingPending: Int,
    ) : MeaningIndexDrainSessionResult

    data class Stopped(
        override val indexedMemories: Int,
        override val remainingPending: Int,
    ) : MeaningIndexDrainSessionResult

    data class EngineUnavailable(
        val reason: String,
        override val indexedMemories: Int = 0,
        override val remainingPending: Int = 0,
    ) : MeaningIndexDrainSessionResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data class SelectionDisagreed(
        val pendingCount: Int,
        override val indexedMemories: Int = 0,
        override val remainingPending: Int = pendingCount,
    ) : MeaningIndexDrainSessionResult {
        init {
            require(pendingCount > 0)
        }
    }

    data object RetryableFailure : MeaningIndexDrainSessionResult {
        override val indexedMemories: Int = 0
        override val remainingPending: Int = 0
    }
}

/**
 * Maps one [RunPendingMeaningIndexResult] onto a drain decision.
 *
 * [RunPendingMeaningIndexResult.EngineUnavailable] and
 * [RunPendingMeaningIndexResult.SelectionDisagreed] must not Continue or
 * Retry — one is a missing model the user must fix; the other is a queue
 * defect. Retrying either is a hot loop.
 */
internal enum class MeaningIndexDrainDecision {
    Continue,
    CompletedDrain,
    StopAndReport,
    RetryableFailure,
    ;

    companion object {
        fun map(result: RunPendingMeaningIndexResult): MeaningIndexDrainDecision = when (result) {
            is RunPendingMeaningIndexResult.Completed -> if (result.hasMore) {
                Continue
            } else {
                CompletedDrain
            }
            RunPendingMeaningIndexResult.NothingPending -> CompletedDrain
            is RunPendingMeaningIndexResult.EngineUnavailable -> StopAndReport
            is RunPendingMeaningIndexResult.SelectionDisagreed -> StopAndReport
        }
    }
}
