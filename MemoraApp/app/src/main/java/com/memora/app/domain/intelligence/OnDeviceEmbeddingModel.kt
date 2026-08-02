package com.memora.app.domain.intelligence

/**
 * Locator for the privately installed on-device embedding model file (ADR-031).
 *
 * Does not download or run inference.
 */
interface OnDeviceEmbeddingModelStore {
    fun isInstalled(): Boolean

    fun absoluteModelPath(): String?

    fun modelIdentity(): ModelVersionIdentity

    fun clear()
}

/** Published MediaPipe average-word embedder (compact, on-device). */
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
