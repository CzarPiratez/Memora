package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasureSyntheticAiPackL2BaselineTest {
    @Test
    fun l2_proves_offline_integrity_edges_without_supporting_available() {
        val report = MeasureSyntheticAiPackL2Baseline()(
            deviceTierId = SyntheticAiPackBaselineCorpus.DEVICE_TIER_JVM_UNIT,
        )

        assertTrue(report.offlineCorePathOk)
        assertTrue(report.priorKnownGoodRetainedAfterCorruptUpdate)
        assertTrue(report.truncatedVerification is AiPackVerificationResult.Rejected)
        assertTrue(
            (report.truncatedVerification as AiPackVerificationResult.Rejected)
                .reason.contains("size", ignoreCase = true),
        )
        assertEquals(CapabilityId.entries.size, report.supportMatrixRows.size)
        assertTrue(report.supportMatrixRows.all { it.tier == CapabilitySupportTier.UNSUPPORTED })
        assertTrue(report.supportMatrixRows.all { !it.mayReportAvailable() })
        assertTrue(report.keywordPathStillRequiresDisclosure)
        assertTrue(
            report.claims.any {
                it.metric == LocalAiBenchmarkMetricId.OFFLINE_CORE_PATH_OK && it.hasMeasuredEvidence
            },
        )
        assertFalse(LocalAiBenchmarkRules.allowsUnmeasuredReleasePromise())
        assertEquals(
            AiPackInstallState.NOT_INSTALLED,
            UnavailableAiPackManager().installationState(SyntheticAiPackBaselineCorpus.PACK_ID),
        )
    }
}
