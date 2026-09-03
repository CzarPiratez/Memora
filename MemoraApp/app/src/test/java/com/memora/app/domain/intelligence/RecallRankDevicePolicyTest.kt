package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecallRankDevicePolicyTest {
    @Test
    fun full_tier_for_arm64_with_six_gb_ram() {
        val tier = RecallRankDevicePolicy.executionTier(
            RecallRankDeviceSnapshot(
                totalRamBytes = 6L * 1024L * 1024L * 1024L,
                primaryAbi = "arm64-v8a",
                isEmulator = false,
            ),
        )
        assertEquals(RecallRankExecutionTier.FULL, tier)
        assertEquals(40, RecallRankDevicePolicy.maxPoolSize(tier))
        assertEquals(128, RecallRankDevicePolicy.maxSequenceLength(tier))
    }

    @Test
    fun reduced_tier_for_arm64_with_four_to_six_gb_ram() {
        val tier = RecallRankDevicePolicy.executionTier(
            RecallRankDeviceSnapshot(
                totalRamBytes = 5L * 1024L * 1024L * 1024L,
                primaryAbi = "arm64-v8a",
                isEmulator = false,
            ),
        )
        assertEquals(RecallRankExecutionTier.REDUCED, tier)
        assertEquals(20, RecallRankDevicePolicy.maxPoolSize(tier))
    }

    @Test
    fun identity_only_for_low_ram_phones() {
        val tier = RecallRankDevicePolicy.executionTier(
            RecallRankDeviceSnapshot(
                totalRamBytes = 3L * 1024L * 1024L * 1024L,
                primaryAbi = "arm64-v8a",
                isEmulator = false,
            ),
        )
        assertEquals(RecallRankExecutionTier.IDENTITY_ONLY, tier)
        assertFalse(
            RecallRankDevicePolicy.shouldAttemptRerank(
                tier = tier,
                rerankPackInstalled = true,
            ),
        )
    }

    @Test
    fun armeabi_v7a_with_enough_ram_gets_reduced_not_identity() {
        val tier = RecallRankDevicePolicy.executionTier(
            RecallRankDeviceSnapshot(
                totalRamBytes = 5L * 1024L * 1024L * 1024L,
                primaryAbi = "armeabi-v7a",
                isEmulator = false,
            ),
        )
        assertEquals(RecallRankExecutionTier.REDUCED, tier)
    }

    @Test
    fun unsupported_abi_is_identity_only() {
        val tier = RecallRankDevicePolicy.executionTier(
            RecallRankDeviceSnapshot(
                totalRamBytes = 8L * 1024L * 1024L * 1024L,
                primaryAbi = "riscv64",
                isEmulator = false,
            ),
        )
        assertEquals(RecallRankExecutionTier.IDENTITY_ONLY, tier)
    }

    @Test
    fun rerank_pack_is_optional_flag_is_true() {
        assertTrue(RecallRankDevicePolicy.RERANK_NOT_REQUIRED_FOR_MEANING_FIND)
    }

    @Test
    fun should_attempt_rerank_only_when_pack_installed_and_tier_allows() {
        assertTrue(
            RecallRankDevicePolicy.shouldAttemptRerank(
                RecallRankExecutionTier.FULL,
                rerankPackInstalled = true,
            ),
        )
        assertFalse(
            RecallRankDevicePolicy.shouldAttemptRerank(
                RecallRankExecutionTier.FULL,
                rerankPackInstalled = false,
            ),
        )
    }
}

class RecallRankCapabilitySupportPolicyTest {
    @Test
    fun midrange_full_tier_is_supported_when_pack_path_exists() {
        val decision = RecallRankCapabilitySupportPolicy.decision(
            deviceClass = DeviceClass.MIDRANGE_ARM64,
            executionTier = RecallRankExecutionTier.FULL,
        )
        assertEquals(CapabilityId.RECALL_RANKER, decision.capability)
        assertEquals(CapabilitySupportTier.SUPPORTED, decision.tier)
    }

    @Test
    fun reduced_tier_is_degraded_explicit() {
        val decision = RecallRankCapabilitySupportPolicy.decision(
            deviceClass = DeviceClass.MIDRANGE_ARM64,
            executionTier = RecallRankExecutionTier.REDUCED,
        )
        assertEquals(CapabilitySupportTier.DEGRADED_EXPLICIT, decision.tier)
        assertFalse(decision.mayReportAvailable())
    }
}
