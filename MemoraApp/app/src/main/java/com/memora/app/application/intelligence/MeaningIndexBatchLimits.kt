package com.memora.app.application.intelligence

/**
 * Per-tap bounds for Build meaning index (enterprise unbounded-work gate).
 *
 * Users may tap again for remaining READY memories. Not a permanent library
 * ceiling and not a measured AVAILABLE claim.
 */
object MeaningIndexBatchLimits {
    const val MAX_MEMORIES_PER_TAP = 25
}
