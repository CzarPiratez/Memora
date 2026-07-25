package com.memora.app.domain.intelligence

/**
 * Spec §4 capability contracts.
 *
 * Operate/inference methods are intentionally deferred until a real on-device
 * adapter exists. This slice only defines replaceable contracts and availability.
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

/** Encodes a Memory and a user query into compatible, versioned semantic vectors. */
interface EmbeddingEngine : LocalCapability {
    override val capabilityId: CapabilityId get() = CapabilityId.EMBEDDING
}

/**
 * Combines deterministic extraction and local observations into a schema-validated
 * Memory. Must not invent unsupported source facts.
 */
interface MemoryBuilder : LocalCapability {
    override val capabilityId: CapabilityId get() = CapabilityId.MEMORY_BUILDER
}

/** Ranks stored candidate Memories and returns the evidence used. */
interface RecallRanker : LocalCapability {
    override val capabilityId: CapabilityId get() = CapabilityId.RECALL_RANKER
}
