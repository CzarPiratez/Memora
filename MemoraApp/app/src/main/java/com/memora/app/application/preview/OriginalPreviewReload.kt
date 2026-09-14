package com.memora.app.application.preview

/**
 * Identity needed to re-open an original at a sharper decode budget after Open.
 *
 * Not a Find path. Notes have no in-app image to reload.
 */
sealed interface OriginalPreviewReloadRequest {
    val sourceId: String
    val sourceAssetKey: String

    data class Pdf(
        override val sourceId: String,
        override val sourceAssetKey: String,
        val pageNumber: Int,
        val documentLabel: String,
    ) : OriginalPreviewReloadRequest {
        init {
            require(sourceId.isNotBlank()) { "PDF reload needs a source id." }
            require(sourceAssetKey.isNotBlank()) { "PDF reload needs a source asset key." }
            require(pageNumber > 0) { "PDF reload needs a positive page." }
            require(documentLabel.isNotBlank()) { "PDF reload needs a document label." }
        }
    }

    data class Photo(
        override val sourceId: String,
        override val sourceAssetKey: String,
        val photoLabel: String,
    ) : OriginalPreviewReloadRequest {
        init {
            require(sourceId.isNotBlank()) { "Photo reload needs a source id." }
            require(sourceAssetKey.isNotBlank()) { "Photo reload needs a source asset key." }
            require(photoLabel.isNotBlank()) { "Photo reload needs a label." }
        }
    }

    data class Screenshot(
        override val sourceId: String,
        override val sourceAssetKey: String,
        val screenshotLabel: String,
    ) : OriginalPreviewReloadRequest {
        init {
            require(sourceId.isNotBlank()) { "Screenshot reload needs a source id." }
            require(sourceAssetKey.isNotBlank()) { "Screenshot reload needs a source asset key." }
            require(screenshotLabel.isNotBlank()) { "Screenshot reload needs a label." }
        }
    }
}

sealed interface OriginalPreviewReloadResult {
    data class Ready(
        val widthPx: Int,
        val heightPx: Int,
        val argb8888: IntArray,
    ) : OriginalPreviewReloadResult {
        init {
            require(widthPx > 0 && heightPx > 0) { "Reload needs positive dimensions." }
            require(argb8888.size == widthPx * heightPx) {
                "Reload pixels must match width × height."
            }
        }
    }

    data object Unavailable : OriginalPreviewReloadResult
}
