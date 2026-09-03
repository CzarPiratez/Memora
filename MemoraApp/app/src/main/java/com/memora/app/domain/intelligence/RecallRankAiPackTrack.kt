package com.memora.app.domain.intelligence

/**
 * Rerank AI Pack (ADR-051 Stage A / FC-02; ADR-052 smart automatic install on eligible tiers).
 *
 * Separate artifact from embedding USE — meaning Find works without rerank weights.
 * Launch UX: installed automatically in unified onboarding when tier is FULL/REDUCED.
 */
object RecallRankAiPackTrack {
    val capability: CapabilityId = CapabilityId.RECALL_RANKER

    const val PLANNED_PACK_ID = "memora-recall-rank-pack-v1"

    /** Soft disclosure ceiling — reject unexpected oversized payloads. */
    const val MAX_DOWNLOAD_BYTES = 64L * 1024L * 1024L

    /** UI disclosure ceiling in whole MiB (weights + tokenizer assets). */
    const val DISCLOSED_SIZE_MB_CEILING = 30

    const val PLANNED_LICENSE = "Apache-2.0 (model); MIT (ONNX Runtime)"

    val PLANNED_MODEL_IDENTITY = OnnxMsMarcoMiniLmCrossEncoderSpec.MODEL_IDENTITY

    /** Planned download size for disclosure until measured artifact hash is frozen. */
    const val PLANNED_DOWNLOAD_SIZE_BYTES: Long = 24L * 1024L * 1024L

    const val PLANNED_STORAGE_REQUIREMENT_BYTES: Long = 28L * 1024L * 1024L

    fun plannedDisclosure(atEpochMs: Long): AiPackDisclosureSnapshot =
        AiPackDisclosureSnapshot(
            packId = PLANNED_PACK_ID,
            capability = capability,
            model = PLANNED_MODEL_IDENTITY,
            downloadSizeBytes = PLANNED_DOWNLOAD_SIZE_BYTES,
            storageRequirementBytes = PLANNED_STORAGE_REQUIREMENT_BYTES,
            license = PLANNED_LICENSE,
            disclosedAtEpochMs = atEpochMs,
        )
}

/**
 * Stage A default semantic head artifact (mobile CPU QInt8 ONNX).
 *
 * Hash pinned after S1 spike on arm64-v8a; not bundled in APK.
 */
object OnnxMsMarcoMiniLmCrossEncoderSpec {
    /**
     * CPU QInt8 derivative of cross-encoder/ms-marco-MiniLM-L-6-v2 (~22 MiB).
     * Must be validated on arm64-v8a during S1 — not x86-only AVX512 builds.
     */
    const val DOWNLOAD_URL =
        "https://huggingface.co/temsa/ms-marco-MiniLM-L-6-v2-onnx-cpu-qint8/" +
            "resolve/main/onnx/model.onnx?download=true"

    const val FILE_NAME = "msmarco_minilm_l6_cross_encoder_qint8_v1.onnx"

    const val UPSTREAM_MODEL_REPO = "cross-encoder/ms-marco-MiniLM-L-6-v2"

    val MODEL_IDENTITY = ModelVersionIdentity(
        modelId = "onnx-msmarco-minilm-l6-cross-encoder",
        version = "qint8-v1",
    )
}
