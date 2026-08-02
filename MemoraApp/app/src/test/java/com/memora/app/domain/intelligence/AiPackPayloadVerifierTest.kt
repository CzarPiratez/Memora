package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPackPayloadVerifierTest {
    private val verifier = AiPackPayloadVerifier()

    @Test
    fun valid_payload_verifies_and_activates_manifest_state() {
        val manifest = SyntheticAiPackBaselineCorpus.validManifest()
        val result = verifier.verify(manifest, SyntheticAiPackBaselineCorpus.validPayload())
        assertTrue(result is AiPackVerificationResult.Verified)
        val verified = result as AiPackVerificationResult.Verified
        assertEquals(AiPackInstallState.ACTIVE, verified.manifest.installationState)
        assertEquals(manifest.packId, verified.manifest.packId)
        assertEquals(AiPackIntegrity.ALGORITHM, "SHA-256")
    }

    @Test
    fun corrupted_payload_is_rejected_and_can_retain_prior_known_good() {
        val manifest = SyntheticAiPackBaselineCorpus.validManifest()
        val result = verifier.verify(
            declaredManifest = manifest,
            payload = SyntheticAiPackBaselineCorpus.corruptedPayload(),
            retainPriorKnownGoodOnFailure = true,
        )
        assertTrue(result is AiPackVerificationResult.Rejected)
        val rejected = result as AiPackVerificationResult.Rejected
        assertTrue(rejected.reason.contains("hash", ignoreCase = true))
        assertTrue(rejected.retainedPriorKnownGood)
    }

    @Test
    fun size_mismatch_is_rejected() {
        val manifest = SyntheticAiPackBaselineCorpus.validManifest()
            .copy(downloadSizeBytes = 1L)
        val result = verifier.verify(manifest, SyntheticAiPackBaselineCorpus.validPayload())
        assertTrue(result is AiPackVerificationResult.Rejected)
        assertTrue(
            (result as AiPackVerificationResult.Rejected).reason.contains("size", ignoreCase = true),
        )
    }

    @Test
    fun empty_payload_is_rejected() {
        val manifest = SyntheticAiPackBaselineCorpus.validManifest()
        val result = verifier.verify(manifest, ByteArray(0))
        assertTrue(result is AiPackVerificationResult.Rejected)
    }
}

class MeasureSyntheticAiPackBaselineTest {
    @Test
    fun measurement_records_aggregate_claims_without_activating_vision() {
        val report = MeasureSyntheticAiPackBaseline()(
            deviceTierId = SyntheticAiPackBaselineCorpus.DEVICE_TIER_JVM_UNIT,
        )
        assertEquals(SyntheticAiPackBaselineCorpus.CORPUS_ID, report.fixtureCorpusId)
        assertEquals(AiPackIntegrity.ALGORITHM, report.integrityAlgorithm)
        assertEquals(4_096L, report.packOnDiskBytes)
        assertEquals(report.disclosedDownloadBytes, report.packOnDiskBytes)
        assertTrue(report.validVerification is AiPackVerificationResult.Verified)
        assertTrue(report.corruptedVerification is AiPackVerificationResult.Rejected)
        assertTrue(report.visionStillUnavailable)
        assertTrue(report.claims.all { it.hasMeasuredEvidence && it.mayPublishAsReleasePromise() })
        assertEquals(
            setOf(
                LocalAiBenchmarkMetricId.PACK_ON_DISK_BYTES,
                LocalAiBenchmarkMetricId.PACK_DOWNLOAD_BYTES,
                LocalAiBenchmarkMetricId.PACK_INTEGRITY_FAILURE_HANDLED,
            ),
            report.claims.map { it.metric }.toSet(),
        )
        // Product stub remains the source of truth for install state.
        assertEquals(
            AiPackInstallState.NOT_INSTALLED,
            UnavailableAiPackManager().installationState(SyntheticAiPackBaselineCorpus.PACK_ID),
        )
        assertFalse(LocalAiBenchmarkRules.allowsUnmeasuredReleasePromise())
    }
}
