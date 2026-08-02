package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPackInstallLedgerTest {
    private val ledger = InMemoryAiPackInstallLedger()

    @Test
    fun disclosure_then_verify_then_active_is_readable_via_manager() {
        ledger.acknowledgeDisclosure(sampleDisclosure())
        ledger.beginVerification(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID, atEpochMs = 2L)
        ledger.recordVerifiedActive(sampleActiveManifest(), atEpochMs = 3L)

        val manager = LedgerBackedAiPackManager(ledger)
        assertEquals(
            AiPackInstallState.ACTIVE,
            manager.installationState(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID),
        )
        val verified = manager.verifiedManifest(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)
        assertTrue(verified is AiPackVerificationResult.Verified)
        assertEquals(
            CapabilityId.EMBEDDING,
            (verified as AiPackVerificationResult.Verified).manifest.capability,
        )
    }

    @Test
    fun blank_ledger_never_reports_active() {
        val manager = LedgerBackedAiPackManager(ledger)
        assertEquals(
            AiPackInstallState.NOT_INSTALLED,
            manager.installationState(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID),
        )
        assertTrue(
            manager.verifiedManifest(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)
                is AiPackVerificationResult.Rejected,
        )
    }

    @Test(expected = IllegalStateException::class)
    fun verification_without_disclosure_is_rejected() {
        ledger.beginVerification(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID, atEpochMs = 1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun activation_without_verifying_state_is_rejected() {
        ledger.acknowledgeDisclosure(sampleDisclosure())
        ledger.recordVerifiedActive(sampleActiveManifest(), atEpochMs = 2L)
    }

    @Test
    fun failed_verify_retains_prior_known_good() {
        ledger.acknowledgeDisclosure(sampleDisclosure())
        ledger.beginVerification(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID, atEpochMs = 2L)
        ledger.recordVerifiedActive(sampleActiveManifest(), atEpochMs = 3L)

        ledger.acknowledgeDisclosure(sampleDisclosure(disclosedAtEpochMs = 4L))
        ledger.beginVerification(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID, atEpochMs = 5L)
        val retained = ledger.recordVerificationFailed(
            packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID,
            reason = "hash mismatch",
            retainPriorKnownGood = true,
            atEpochMs = 6L,
        )

        assertEquals(AiPackInstallState.ACTIVE, retained.installationState)
        assertEquals("sha256:good", retained.verifiedIntegrityHash)
        assertTrue(
            LedgerBackedAiPackManager(ledger)
                .verifiedManifest(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)
                is AiPackVerificationResult.Verified,
        )
    }

    @Test
    fun failed_first_install_marks_failed_verification() {
        ledger.acknowledgeDisclosure(sampleDisclosure())
        ledger.beginVerification(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID, atEpochMs = 2L)
        val failed = ledger.recordVerificationFailed(
            packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID,
            reason = "truncated payload",
            retainPriorKnownGood = true,
            atEpochMs = 3L,
        )
        assertEquals(AiPackInstallState.FAILED_VERIFICATION, failed.installationState)
        assertTrue(
            LedgerBackedAiPackManager(ledger)
                .verifiedManifest(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)
                is AiPackVerificationResult.Rejected,
        )
    }

    @Test
    fun track_targets_embedding_capability() {
        assertEquals(CapabilityId.EMBEDDING, EmbeddingFirstAiPackTrack.capability)
        assertTrue(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID.startsWith("memora-embedding"))
    }

    private fun sampleDisclosure(
        disclosedAtEpochMs: Long = 1L,
    ): AiPackDisclosureSnapshot = AiPackDisclosureSnapshot(
        packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID,
        capability = EmbeddingFirstAiPackTrack.capability,
        model = ModelVersionIdentity(modelId = "embedding-tbd", version = "0.0.0-planned"),
        downloadSizeBytes = 10_000_000L,
        storageRequirementBytes = 12_000_000L,
        license = "TBD-pack-license",
        disclosedAtEpochMs = disclosedAtEpochMs,
    )

    private fun sampleActiveManifest(): AiPackManifest = AiPackManifest(
        packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID,
        capability = EmbeddingFirstAiPackTrack.capability,
        model = ModelVersionIdentity(modelId = "embedding-tbd", version = "0.0.0-planned"),
        compatibleAppVersions = "1.0.0+",
        compatibleSchemaVersions = "memory-schema-1",
        downloadSizeBytes = 10_000_000L,
        storageRequirementBytes = 12_000_000L,
        integrityHash = "sha256:good",
        license = "TBD-pack-license",
        installationState = AiPackInstallState.ACTIVE,
    )
}
