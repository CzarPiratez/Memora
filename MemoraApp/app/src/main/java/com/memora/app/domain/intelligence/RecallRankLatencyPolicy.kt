package com.memora.app.domain.intelligence

/**
 * Stage A midrange latency budget (FC-02 S3 / ADR-051).
 *
 * Planning bar: aggregate rerank wall for a FULL pool (40) ≤ [TARGET_AGGREGATE_WALL_MS],
 * or honest [MidrangeLatencyDisposition.DEGRADED_EXPLICIT] with reduced pool and/or
 * identity fallback. Does not authorize AVAILABLE.
 */
object RecallRankLatencyPolicy {
    const val TARGET_AGGREGATE_WALL_MS = 800L

    /**
     * S1 Samsung SM-A156E evidence (dummy tokens, pool 40, seqLen 128): 1642 ms.
     * S3 re-measures with real WordPiece; disposition uses the latest measured wall.
     */
    const val S1_FULL_POOL_WALL_MS_EVIDENCE = 1642L

    enum class MidrangeLatencyDisposition {
        /** Full pool 40 fits the planning bar. */
        WITHIN_BUDGET,

        /**
         * Full pool over budget — Stage A ships with reduced effective pool and
         * disclosed degraded latency posture (not silent).
         */
        DEGRADED_EXPLICIT,

        /**
         * Even reduced pool cannot meet budget — prefer identity ranking for that
         * query / host class until a faster pack is measured.
         */
        IDENTITY_FALLBACK,
    }

    fun dispositionForMeasuredWallMs(
        fullPoolWallMs: Long,
        reducedPoolWallMs: Long? = null,
    ): MidrangeLatencyDisposition {
        require(fullPoolWallMs >= 0L)
        if (fullPoolWallMs <= TARGET_AGGREGATE_WALL_MS) {
            return MidrangeLatencyDisposition.WITHIN_BUDGET
        }
        if (reducedPoolWallMs != null && reducedPoolWallMs <= TARGET_AGGREGATE_WALL_MS) {
            return MidrangeLatencyDisposition.DEGRADED_EXPLICIT
        }
        if (reducedPoolWallMs == null) {
            // Full over budget; reduced not yet measured — degrade explicitly (S1 path).
            return MidrangeLatencyDisposition.DEGRADED_EXPLICIT
        }
        return MidrangeLatencyDisposition.IDENTITY_FALLBACK
    }

    /**
     * Pool size Stage A should use for [tier] under [disposition].
     * Capacity ceilings still come from [RecallRankDevicePolicy].
     */
    fun effectivePoolSize(
        tier: RecallRankExecutionTier,
        disposition: MidrangeLatencyDisposition,
    ): Int {
        val capacity = RecallRankDevicePolicy.maxPoolSize(tier)
        if (capacity == 0) return 0
        return when (disposition) {
            MidrangeLatencyDisposition.WITHIN_BUDGET -> capacity
            MidrangeLatencyDisposition.DEGRADED_EXPLICIT ->
                minOf(capacity, RecallRankDevicePolicy.REDUCED_POOL_SIZE)
            MidrangeLatencyDisposition.IDENTITY_FALLBACK -> 0
        }
    }

    fun shouldFallBackToIdentityForWallMs(aggregateWallMs: Long): Boolean =
        aggregateWallMs > TARGET_AGGREGATE_WALL_MS

/**
 * Provisional / measured midrange disposition for Stage A wire.
 *
 * Updated after S3 device PASS (2026-09-03 SM-A156E): full40=1517 ms,
 * reduced20@96=584 ms → [MidrangeLatencyDisposition.DEGRADED_EXPLICIT].
 */
fun measuredMidrangeDisposition(): MidrangeLatencyDisposition =
    dispositionForMeasuredWallMs(
        fullPoolWallMs = 1517L,
        reducedPoolWallMs = 584L,
    )

/** Provisional disposition from S1 only (superseded by [measuredMidrangeDisposition] after S3). */
fun provisionalDispositionFromS1Evidence(): MidrangeLatencyDisposition =
    dispositionForMeasuredWallMs(fullPoolWallMs = S1_FULL_POOL_WALL_MS_EVIDENCE)
}
