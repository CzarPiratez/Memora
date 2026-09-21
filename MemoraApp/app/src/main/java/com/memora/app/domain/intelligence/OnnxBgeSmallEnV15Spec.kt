package com.memora.app.domain.intelligence

/**
 * ADR-055 slice 3 challenger pack: BGE-small-en-v1.5 (ONNX uint8/q8).
 *
 * Probe-only. Does **not** replace the product [EmbeddingEngine] (USE).
 * Does **not** write Room meaning vectors. Swap remains slice 4 and only
 * if [com.memora.app.application.intelligence.MeaningEncoderBakeOffVerdict]
 * says challenger_wins.
 *
 * Artifact: Xenova/bge-small-en-v1.5 `onnx/model_quantized.onnx`
 * (~34 MiB; sha256 pinned below). Inference uses the existing ONNX Runtime
 * Android dependency (already on the classpath for Stage A).
 */
object OnnxBgeSmallEnV15Spec {
    const val DOWNLOAD_URL =
        "https://huggingface.co/Xenova/bge-small-en-v1.5/resolve/main/" +
            "onnx/model_quantized.onnx?download=true"

    const val FILE_NAME = "bge_small_en_v15_quantized_v1.onnx"

    const val UPSTREAM_MODEL_REPO = "BAAI/bge-small-en-v1.5"

    const val ONNX_SOURCE_REPO = "Xenova/bge-small-en-v1.5"

    /** Published LFS size for model_quantized.onnx (bytes). */
    const val EXPECTED_BYTES = 34_014_426L

    const val EXPECTED_SHA256 =
        "6c9c6101a956d62dfb5e7190c538226c0c5bb9cb27b651234b6df063ee7dbfe4"

    /** Soft disclosure ceiling — reject unexpected oversized payloads. */
    const val MAX_DOWNLOAD_BYTES = 48L * 1024L * 1024L

    const val DISCLOSED_SIZE_MB_CEILING = 40

    const val EMBEDDING_DIMENSIONS = 384

    const val MAX_SEQUENCE_LENGTH = 128

    /**
     * BGE v1.5 retrieval instruction for **queries** only. Passage / Memory
     * text is encoded without a prefix (upstream model card).
     */
    const val QUERY_PREFIX =
        "Represent this sentence for searching relevant passages: "

    val MODEL_IDENTITY = ModelVersionIdentity(
        modelId = "onnx-bge-small-en-v1.5",
        version = "quantized-v1",
    )

    const val PACK_ID = "memora-meaning-encoder-challenger-bge-small-v1"

    const val LICENSE = "MIT (BAAI/bge-small-en-v1.5); MIT (ONNX Runtime)"
}
