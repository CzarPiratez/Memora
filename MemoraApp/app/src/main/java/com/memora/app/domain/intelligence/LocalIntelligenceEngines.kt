package com.memora.app.domain.intelligence

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.memory.AssetMemoryFact
import java.time.Instant

/**
 * Spec §4 capability contracts.
 *
 * [MemoryBuilder.assemble] is the frozen Asset Memory construction seam (MIG-04).
 * Other engines may still defer operate methods until an approved on-device adapter
 * exists.
 */

/** Structured, evidence-citable image observations (scene, objects, summary). */
interface VisionEngine : LocalCapability {
    override val capabilityId: CapabilityId get() = CapabilityId.VISION
}

/** Text plus location/provenance suitable for citations. */
interface OcrEngine : LocalCapability {
    override val capabilityId: CapabilityId get() = CapabilityId.OCR
}

/**
 * PDF or note understanding from deterministic extracted text.
 *
 * Does not decide source access.
 */
interface DocumentEngine : LocalCapability {
    override val capabilityId: CapabilityId get() = CapabilityId.DOCUMENT
}

/**
 * Encodes Memory text and user queries into compatible, versioned semantic vectors.
 *
 * Callers must honor [availability] — Unavailable engines must not invent vectors.
 */
interface EmbeddingEngine : LocalCapability {
    override val capabilityId: CapabilityId get() = CapabilityId.EMBEDDING

    fun embedText(text: String): EmbeddingEncodeResult
}

/**
 * Combines deterministic extraction and local observations into a schema-validated
 * Memory. Must not invent unsupported source facts (Local AI Spec §4).
 *
 * Persistence is not this contract's job — return a validated [Memory] (or a pure
 * build outcome). Callers must honor [availability].
 */
interface MemoryBuilder : LocalCapability {
    override val capabilityId: CapabilityId get() = CapabilityId.MEMORY_BUILDER

    /**
     * Builds a schema-validated Asset Memory from already-extracted facts plus
     * optional local observations (empty until VisionEngine is authorized).
     *
     * @param assetIdentity Memory must bind to this Asset identity
     * @param assetFingerprint Memory must bind to this Asset version fingerprint
     * @param facts Deterministic extraction facts (sanitized by the implementation)
     * @param localObservations Future VALIDATED_OBSERVATION inputs; default empty
     * @param createdAt Instant used for createdAt/updatedAt (tests may fix the clock)
     */
    fun assemble(
        assetIdentity: AssetIdentity,
        assetFingerprint: AssetFingerprint,
        facts: List<AssetMemoryFact>,
        localObservations: List<LocalObservation> = emptyList(),
        createdAt: Instant,
    ): MemoryBuildResult
}

/** Ranks stored candidate Memories and returns the evidence used. */
interface RecallRanker : LocalCapability {
    override val capabilityId: CapabilityId get() = CapabilityId.RECALL_RANKER
}
