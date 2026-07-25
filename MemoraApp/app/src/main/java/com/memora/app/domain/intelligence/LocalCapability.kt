package com.memora.app.domain.intelligence

/**
 * Shared Local Intelligence Layer surface required by every Spec §4 engine.
 *
 * Availability-first: callers must check [availability] before requesting work.
 * Implementations live in data/platform adapters; this package stays free of
 * Android, vendor SDKs, and network types.
 */
interface LocalCapability {
    val capabilityId: CapabilityId

    fun availability(): CapabilityAvailability

    /** Limits when available; null when the capability is unavailable. */
    fun limits(): CapabilityLimits?
}
