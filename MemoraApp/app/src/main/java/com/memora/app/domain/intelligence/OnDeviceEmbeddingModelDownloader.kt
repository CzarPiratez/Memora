package com.memora.app.domain.intelligence

/**
 * Downloads and installs the product on-device embedding model (USE / ADR-032).
 *
 * Network and filesystem details stay in platform adapters.
 */
interface OnDeviceEmbeddingModelDownloader {
    fun downloadProductModel(): OnDeviceEmbeddingModelDownloadOutcome
}

sealed interface OnDeviceEmbeddingModelDownloadOutcome {
    data object Installed : OnDeviceEmbeddingModelDownloadOutcome

    data class Failed(val reason: String) : OnDeviceEmbeddingModelDownloadOutcome {
        init {
            require(reason.isNotBlank())
        }
    }
}
