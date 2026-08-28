package com.memora.app.domain.intelligence

import com.memora.app.domain.memory.Memory

/**
 * Provenance-ready placeholder for a future validated local model observation
 * (VisionEngine → [com.memora.app.domain.memory.MemoryEvidenceClass.VALIDATED_OBSERVATION]).
 *
 * MIG-04 freezes the MemoryBuilder seam that accepts this list. Deterministic
 * assembly rejects non-empty lists rather than silently dropping them. Do not
 * invent Vision-specific fields here until that capability is authorized.
 */
data class LocalObservation(
    /** Stable caller-supplied id for future evidence citation. */
    val observationId: String,
) {
    init {
        require(observationId.isNotBlank()) {
            "A local observation needs a non-blank id for future provenance."
        }
    }
}

/**
 * Pure Memory construction outcome from [MemoryBuilder.assemble].
 *
 * Persistence, asset lookup, and AlreadyPresent stay in the application use case.
 */
sealed interface MemoryBuildResult {
    data class Success(val memory: Memory) : MemoryBuildResult

    data object NoUsableEvidence : MemoryBuildResult

    /**
     * Deterministic builder (MIG-04) does not yet consume local observations.
     * Callers must not treat this as success or invent a Memory.
     */
    data class ObservationsUnsupported(
        val reason: String,
    ) : MemoryBuildResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Unavailable(
        val reason: String,
    ) : MemoryBuildResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
