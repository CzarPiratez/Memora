package com.memora.app.domain.intelligence

/**
 * Downloads the ADR-055 slice 3 challenger embedder (BGE-small ONNX).
 * Probe-only — not the product meaning pack.
 */
interface MeaningEncoderChallengerDownloader {
    fun downloadChallengerModel(): OnDeviceEmbeddingModelDownloadOutcome
}
