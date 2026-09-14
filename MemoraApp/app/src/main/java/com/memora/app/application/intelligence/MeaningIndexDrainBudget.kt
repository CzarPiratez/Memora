package com.memora.app.application.intelligence

/**
 * Whether this worker invocation should start another [RunPendingMeaningIndex]
 * batch. Always runs the first batch even if the budget is already zero, so a
 * mis-set clock cannot skip work. After that, elapsed time must stay under
 * [MeaningIndexBatchLimits.WORKER_WALL_CLOCK_BUDGET_MS].
 */
object MeaningIndexDrainBudget {
    fun anotherBatchFits(
        startedAtMs: Long,
        nowMs: Long,
        batchesAlreadyRun: Int,
        budgetMs: Long = MeaningIndexBatchLimits.WORKER_WALL_CLOCK_BUDGET_MS,
    ): Boolean {
        require(startedAtMs >= 0)
        require(nowMs >= startedAtMs) { "Clock went backwards during a meaning-index drain." }
        require(batchesAlreadyRun >= 0)
        require(budgetMs > 0)
        if (batchesAlreadyRun == 0) return true
        return nowMs - startedAtMs < budgetMs
    }
}
