package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitySupportDecisionTest {
    @Test
    fun supported_may_report_available() {
        val decision = CapabilitySupportDecision(
            capability = CapabilityId.OCR,
            deviceClass = DeviceClass.MIDRANGE_ARM64,
            tier = CapabilitySupportTier.SUPPORTED,
            reason = "Verified OCR pack active.",
        )
        assertTrue(decision.mayReportAvailable())
    }

    @Test
    fun unsupported_must_not_report_available() {
        val decision = CapabilitySupportDecision(
            capability = CapabilityId.VISION,
            deviceClass = DeviceClass.LOW_RAM,
            tier = CapabilitySupportTier.UNSUPPORTED,
            reason = "Pack exceeds safe memory budget.",
        )
        assertFalse(decision.mayReportAvailable())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_blank_reason() {
        CapabilitySupportDecision(
            capability = CapabilityId.EMBEDDING,
            deviceClass = DeviceClass.EMULATOR_MEDIUM_PHONE,
            tier = CapabilitySupportTier.UNSUPPORTED,
            reason = " ",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun degraded_requires_explicit_limitation_text() {
        CapabilitySupportDecision(
            capability = CapabilityId.DOCUMENT,
            deviceClass = DeviceClass.MIDRANGE_ARM64,
            tier = CapabilitySupportTier.DEGRADED_EXPLICIT,
            reason = "short",
        )
    }
}

class DefaultUnsupportedCapabilitySupportPolicyTest {
    @Test
    fun every_spec_capability_defaults_to_unsupported() {
        val policy = DefaultUnsupportedCapabilitySupportPolicy()
        CapabilityId.entries.forEach { capability ->
            DeviceClass.entries.forEach { deviceClass ->
                val decision = policy.decision(capability, deviceClass)
                assertEquals(CapabilitySupportTier.UNSUPPORTED, decision.tier)
                assertFalse(decision.mayReportAvailable())
                assertTrue(decision.reason.isNotBlank())
            }
        }
    }
}

class SemanticFallbackRulesTest {
    @Test
    fun forbids_silent_filename_and_unlabeled_keyword_as_semantic() {
        assertFalse(SemanticFallbackRules.allowsSilentFilenameFallback())
        assertFalse(SemanticFallbackRules.allowsUnlabeledKeywordAsSemantic())
        assertTrue(SemanticFallbackRules.keywordPathDisclosureRequired())
    }
}
