package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecallRankLatencyPolicyTest {
    @Test
    fun within_budget_when_full_pool_under_800ms() {
        val d = RecallRankLatencyPolicy.dispositionForMeasuredWallMs(fullPoolWallMs = 750L)
        assertEquals(RecallRankLatencyPolicy.MidrangeLatencyDisposition.WITHIN_BUDGET, d)
        assertEquals(
            40,
            RecallRankLatencyPolicy.effectivePoolSize(RecallRankExecutionTier.FULL, d),
        )
    }

    @Test
    fun degraded_explicit_when_full_over_budget_but_reduced_fits() {
        val d = RecallRankLatencyPolicy.dispositionForMeasuredWallMs(
            fullPoolWallMs = 1642L,
            reducedPoolWallMs = 700L,
        )
        assertEquals(RecallRankLatencyPolicy.MidrangeLatencyDisposition.DEGRADED_EXPLICIT, d)
        assertEquals(
            20,
            RecallRankLatencyPolicy.effectivePoolSize(RecallRankExecutionTier.FULL, d),
        )
    }

    @Test
    fun identity_fallback_when_reduced_also_over_budget() {
        val d = RecallRankLatencyPolicy.dispositionForMeasuredWallMs(
            fullPoolWallMs = 1642L,
            reducedPoolWallMs = 900L,
        )
        assertEquals(RecallRankLatencyPolicy.MidrangeLatencyDisposition.IDENTITY_FALLBACK, d)
        assertEquals(
            0,
            RecallRankLatencyPolicy.effectivePoolSize(RecallRankExecutionTier.FULL, d),
        )
        assertTrue(RecallRankLatencyPolicy.shouldFallBackToIdentityForWallMs(900L))
    }

    @Test
    fun s3_device_evidence_is_degraded_explicit_with_pool_20() {
        val d = RecallRankLatencyPolicy.measuredMidrangeDisposition()
        assertEquals(RecallRankLatencyPolicy.MidrangeLatencyDisposition.DEGRADED_EXPLICIT, d)
        assertEquals(
            20,
            RecallRankLatencyPolicy.effectivePoolSize(RecallRankExecutionTier.FULL, d),
        )
    }

    @Test
    fun s1_evidence_alone_is_degraded_explicit_until_reduced_measured() {
        val d = RecallRankLatencyPolicy.provisionalDispositionFromS1Evidence()
        assertEquals(RecallRankLatencyPolicy.MidrangeLatencyDisposition.DEGRADED_EXPLICIT, d)
        assertFalse(RecallRankLatencyPolicy.shouldFallBackToIdentityForWallMs(800L))
        assertTrue(RecallRankLatencyPolicy.shouldFallBackToIdentityForWallMs(801L))
    }
}
