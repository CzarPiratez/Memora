package com.memora.app.domain.intelligence

/**
 * Planning device class for Local-AI compatibility matrix rows.
 *
 * Concrete ABI/API/RAM cut lines are filled when packs and benchmarks exist.
 */
enum class DeviceClass {
    EMULATOR_MEDIUM_PHONE,
    MIDRANGE_ARM64,
    LOW_RAM,
    UNSUPPORTED_ABI,
}

/**
 * Support tier for one capability on one device class.
 *
 * [DEGRADED_EXPLICIT] requires disclosed limitation text; it is never a silent
 * substitute for full semantic understanding.
 */
enum class CapabilitySupportTier {
    SUPPORTED,
    DEGRADED_EXPLICIT,
    UNSUPPORTED,
}

/**
 * One matrix row: capability × device class → tier + honest reason.
 */
data class CapabilitySupportDecision(
    val capability: CapabilityId,
    val deviceClass: DeviceClass,
    val tier: CapabilitySupportTier,
    val reason: String,
) {
    init {
        require(reason.isNotBlank()) {
            "A support decision must explain the tier with a non-blank reason."
        }
        if (tier == CapabilitySupportTier.DEGRADED_EXPLICIT) {
            require(reason.length >= 12) {
                "Degraded support needs an explicit limitation reason."
            }
        }
    }

    fun mayReportAvailable(): Boolean = tier == CapabilitySupportTier.SUPPORTED
}

/**
 * Resolves support for Spec §4 capabilities without inventing availability.
 */
interface CapabilitySupportPolicy {
    fun decision(capability: CapabilityId, deviceClass: DeviceClass): CapabilitySupportDecision
}

/**
 * Gate default: every intelligence capability is unsupported until measured rows
 * and a verified pack/runtime bind later.
 */
class DefaultUnsupportedCapabilitySupportPolicy(
    private val reason: String = DEFAULT_UNSUPPORTED_REASON,
) : CapabilitySupportPolicy {
    override fun decision(
        capability: CapabilityId,
        deviceClass: DeviceClass,
    ): CapabilitySupportDecision = CapabilitySupportDecision(
        capability = capability,
        deviceClass = deviceClass,
        tier = CapabilitySupportTier.UNSUPPORTED,
        reason = reason,
    )
}

/**
 * Guards against Spec §12 silent fallback from semantic understanding to
 * filename-only or unlabeled keyword search.
 */
object SemanticFallbackRules {
    fun allowsSilentFilenameFallback(): Boolean = false

    fun allowsUnlabeledKeywordAsSemantic(): Boolean = false

    fun keywordPathDisclosureRequired(): Boolean = true
}

private const val DEFAULT_UNSUPPORTED_REASON =
    "On-device intelligence is not supported on this phone yet."
