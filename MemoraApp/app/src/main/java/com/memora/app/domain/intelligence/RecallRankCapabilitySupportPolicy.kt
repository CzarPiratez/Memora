package com.memora.app.domain.intelligence

/**
 * Planning-matrix rows for [CapabilityId.RECALL_RANKER] (ADR-051 / FC-02 Stage A).
 *
 * Meaning Find does not depend on this capability; optional rerank assist only.
 */
object RecallRankCapabilitySupportPolicy {
    fun decision(
        deviceClass: DeviceClass,
        executionTier: RecallRankExecutionTier,
    ): CapabilitySupportDecision {
        val tier = when {
            executionTier == RecallRankExecutionTier.IDENTITY_ONLY ->
                CapabilitySupportTier.UNSUPPORTED
            deviceClass == DeviceClass.UNSUPPORTED_ABI ->
                CapabilitySupportTier.UNSUPPORTED
            deviceClass == DeviceClass.LOW_RAM ->
                CapabilitySupportTier.DEGRADED_EXPLICIT
            executionTier == RecallRankExecutionTier.REDUCED ->
                CapabilitySupportTier.DEGRADED_EXPLICIT
            else -> CapabilitySupportTier.SUPPORTED
        }
        return CapabilitySupportDecision(
            capability = CapabilityId.RECALL_RANKER,
            deviceClass = deviceClass,
            tier = tier,
            reason = reasonFor(deviceClass, executionTier, tier),
        )
    }

    fun mapDeviceClass(snapshot: RecallRankDeviceSnapshot): DeviceClass = when {
        snapshot.isEmulator -> DeviceClass.EMULATOR_MEDIUM_PHONE
        snapshot.primaryAbi == "arm64-v8a" -> DeviceClass.MIDRANGE_ARM64
        snapshot.primaryAbi == "armeabi-v7a" -> DeviceClass.LOW_RAM
        snapshot.primaryAbi != null -> DeviceClass.UNSUPPORTED_ABI
        else -> DeviceClass.MIDRANGE_ARM64
    }

    private fun reasonFor(
        deviceClass: DeviceClass,
        executionTier: RecallRankExecutionTier,
        tier: CapabilitySupportTier,
    ): String = when {
        executionTier == RecallRankExecutionTier.IDENTITY_ONLY ->
            "This phone keeps meaning search without ranking assist; optional rerank pack is not advised."
        deviceClass == DeviceClass.UNSUPPORTED_ABI ->
            "Ranking assist is not available on this processor type."
        tier == CapabilitySupportTier.DEGRADED_EXPLICIT ->
            "Ranking assist uses a smaller candidate pool on this phone for speed and memory."
        else ->
            "Ranking assist is supported when the optional rerank pack is installed."
    }
}
