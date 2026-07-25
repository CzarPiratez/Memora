package com.memora.app.domain.intelligence

/**
 * Stable identifiers for Local Intelligence Layer capabilities (Local AI Spec §4).
 *
 * Domain and application code depend on these contracts, never on a named model,
 * vendor SDK, or network client.
 */
enum class CapabilityId {
    VISION,
    OCR,
    DOCUMENT,
    EMBEDDING,
    MEMORY_BUILDER,
    RECALL_RANKER,
}

/**
 * Versioned model identity for an available on-device capability.
 *
 * Blank IDs are rejected so callers cannot claim an anonymous “working” model.
 */
data class ModelVersionIdentity(
    val modelId: String,
    val version: String,
) {
    init {
        require(modelId.isNotBlank()) { "A model identity needs a non-blank model id." }
        require(version.isNotBlank()) { "A model identity needs a non-blank version." }
    }
}

/**
 * Truthful availability of one local capability.
 *
 * [Unavailable] must carry a plain reason. No evidence means no AVAILABLE claim.
 */
sealed interface CapabilityAvailability {
    data class Available(
        val model: ModelVersionIdentity,
    ) : CapabilityAvailability

    data class Unavailable(
        val reason: String,
    ) : CapabilityAvailability {
        init {
            require(reason.isNotBlank()) {
                "An unavailable capability must explain why it cannot run."
            }
        }
    }
}

/**
 * Optional bounded limits advertised when a capability is available.
 *
 * Stubs that are unavailable return null rather than inventing limits.
 */
data class CapabilityLimits(
    val maxInputBytes: Long?,
    val maxOutputItems: Int?,
) {
    init {
        require(maxInputBytes == null || maxInputBytes > 0) {
            "A positive maxInputBytes is required when set."
        }
        require(maxOutputItems == null || maxOutputItems > 0) {
            "A positive maxOutputItems is required when set."
        }
    }
}
