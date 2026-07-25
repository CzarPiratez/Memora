package com.memora.app.domain.intelligence

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiBenchmarkContractsTest {
    @Test
    fun unmeasured_claim_must_not_publish_as_release_promise() {
        val claim = LocalAiBenchmarkClaim(
            metric = LocalAiBenchmarkMetricId.SEARCH_LATENCY_MS,
            deviceTierId = "emulator_medium_phone",
            fixtureCorpusId = "synthetic_v0",
            hasMeasuredEvidence = false,
        )
        assertFalse(claim.mayPublishAsReleasePromise())
        assertFalse(LocalAiBenchmarkRules.allowsUnmeasuredReleasePromise())
    }

    @Test
    fun measured_claim_may_publish_as_release_promise() {
        val claim = LocalAiBenchmarkClaim(
            metric = LocalAiBenchmarkMetricId.OFFLINE_CORE_PATH_OK,
            deviceTierId = "midrange_arm64",
            fixtureCorpusId = "synthetic_v0",
            hasMeasuredEvidence = true,
        )
        assertTrue(claim.mayPublishAsReleasePromise())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_blank_device_tier() {
        LocalAiBenchmarkClaim(
            metric = LocalAiBenchmarkMetricId.RECALL_AT_K,
            deviceTierId = " ",
            fixtureCorpusId = "synthetic_v0",
            hasMeasuredEvidence = true,
        )
    }
}
