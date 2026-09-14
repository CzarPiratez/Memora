package com.memora.app.application.intelligence

/**
 * Bounds for one meaning-index batch and one worker invocation.
 *
 * The count cap is a backstop so a single [RunPendingMeaningIndex] call cannot
 * embed the whole library. The wall-clock budget is a second backstop so a
 * WorkManager chain of 25-memory nodes cannot grow to sixty jobs on a 1,500
 * memory library (I3 + I4-lite). Neither figure is a measured AVAILABLE SLA.
 *
 * I4's *measured* per-item cost is still open; 4 minutes is a safety margin
 * under WorkManager's ~10 minute execution ceiling, not a device benchmark.
 */
object MeaningIndexBatchLimits {
    const val MAX_MEMORIES_PER_TAP = 25

    /** Wall-clock budget for one worker invocation, then re-enqueue. */
    const val WORKER_WALL_CLOCK_BUDGET_MS = 4 * 60 * 1000L
}
