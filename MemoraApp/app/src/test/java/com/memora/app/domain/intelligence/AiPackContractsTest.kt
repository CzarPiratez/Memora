package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPackManifestTest {
    @Test
    fun valid_manifest_retains_required_fields() {
        val manifest = sampleManifest()
        assertEquals("vision-pack-1", manifest.packId)
        assertEquals(CapabilityId.VISION, manifest.capability)
        assertEquals("vision-a", manifest.model.modelId)
        assertEquals(AiPackInstallState.NOT_INSTALLED, manifest.installationState)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_blank_integrity_hash() {
        sampleManifest(integrityHash = " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_non_positive_download_size() {
        sampleManifest(downloadSizeBytes = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_blank_license() {
        sampleManifest(license = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun verified_result_rejects_non_active_state() {
        AiPackVerificationResult.Verified(
            sampleManifest(installationState = AiPackInstallState.NOT_INSTALLED),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejected_result_rejects_blank_reason() {
        AiPackVerificationResult.Rejected(reason = " ", retainedPriorKnownGood = true)
    }

    private fun sampleManifest(
        integrityHash: String = "abc123",
        downloadSizeBytes: Long = 1_024L,
        license: String = "Apache-2.0",
        installationState: AiPackInstallState = AiPackInstallState.NOT_INSTALLED,
    ): AiPackManifest = AiPackManifest(
        packId = "vision-pack-1",
        capability = CapabilityId.VISION,
        model = ModelVersionIdentity(modelId = "vision-a", version = "1.0.0"),
        compatibleAppVersions = "1.0.0+",
        compatibleSchemaVersions = "memory-schema-1",
        downloadSizeBytes = downloadSizeBytes,
        storageRequirementBytes = 2_048L,
        integrityHash = integrityHash,
        license = license,
        installationState = installationState,
    )
}

class UnavailableAiPackManagerTest {
    @Test
    fun stub_never_reports_active_or_verified_pack() {
        val manager = UnavailableAiPackManager()
        assertEquals(AiPackInstallState.NOT_INSTALLED, manager.installationState("any-pack"))

        val result = manager.verifiedManifest("any-pack")
        assertTrue(result is AiPackVerificationResult.Rejected)
        val rejected = result as AiPackVerificationResult.Rejected
        assertTrue(rejected.reason.isNotBlank())
        assertFalse(rejected.retainedPriorKnownGood)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stub_rejects_blank_pack_id() {
        UnavailableAiPackManager().installationState(" ")
    }
}
