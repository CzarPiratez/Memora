package com.memora.app.domain.intelligence

/**
 * Locator for the privately installed on-device embedding model file (ADR-031/032).
 *
 * Does not download or run inference.
 */
interface OnDeviceEmbeddingModelStore {
    fun isInstalled(): Boolean

    fun absoluteModelPath(): String?

    fun modelIdentity(): ModelVersionIdentity

    fun clear()
}

/**
 * Product on-device embedder (E4b / ADR-032): MediaPipe Universal Sentence Encoder.
 *
 * Replaces the compact average-word model for semantic-only quality. Model is
 * never bundled in the APK.
 */
object MediaPipeUniversalSentenceEncoderSpec {
    const val DOWNLOAD_URL =
        "https://storage.googleapis.com/mediapipe-models/text_embedder/" +
            "universal_sentence_encoder/float32/1/universal_sentence_encoder.tflite"

    const val FILE_NAME = "universal_sentence_encoder_float32_1.tflite"

    /**
     * Soft disclosure ceiling — reject unexpected oversized payloads.
     * Published USE float32 is typically ~30 MiB; ceiling leaves headroom.
     */
    const val MAX_DOWNLOAD_BYTES = 64L * 1024L * 1024L

    /** UI disclosure ceiling in whole MiB (honest soft upper bound). */
    const val DISCLOSED_SIZE_MB_CEILING = 40

    val MODEL_IDENTITY = ModelVersionIdentity(
        modelId = "mediapipe-universal-sentence-encoder",
        version = "float32-1",
    )
}

/**
 * Legacy compact average-word embedder (ADR-031 first ship / M2 baseline).
 *
 * Kept for measurement harnesses and upgrade cleanup. Not the product default.
 */
object MediaPipeAverageWordEmbedderSpec {
    const val DOWNLOAD_URL =
        "https://storage.googleapis.com/mediapipe-models/text_embedder/" +
            "average_word_embedder/float32/1/average_word_embedder.tflite"

    const val FILE_NAME = "average_word_embedder_float32_1.tflite"

    /** Soft disclosure ceiling (~5 MiB) — reject larger unexpected payloads. */
    const val MAX_DOWNLOAD_BYTES = 8L * 1024L * 1024L

    val MODEL_IDENTITY = ModelVersionIdentity(
        modelId = "mediapipe-average-word-embedder",
        version = "float32-1",
    )
}
