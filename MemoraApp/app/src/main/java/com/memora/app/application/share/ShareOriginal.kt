package com.memora.app.application.share

/**
 * Identity needed to hand a stored original to an app the person chooses.
 *
 * Not a Find path. Not Act. Notes have no local file URI in this slice.
 */
sealed interface ShareOriginalRequest {
    val sourceId: String
    val sourceAssetKey: String
    val label: String

    data class Pdf(
        override val sourceId: String,
        override val sourceAssetKey: String,
        override val label: String,
    ) : ShareOriginalRequest {
        init {
            require(sourceId.isNotBlank() && sourceAssetKey.isNotBlank() && label.isNotBlank())
        }
    }

    data class Photo(
        override val sourceId: String,
        override val sourceAssetKey: String,
        override val label: String,
    ) : ShareOriginalRequest {
        init {
            require(sourceId.isNotBlank() && sourceAssetKey.isNotBlank() && label.isNotBlank())
        }
    }

    data class Screenshot(
        override val sourceId: String,
        override val sourceAssetKey: String,
        override val label: String,
    ) : ShareOriginalRequest {
        init {
            require(sourceId.isNotBlank() && sourceAssetKey.isNotBlank() && label.isNotBlank())
        }
    }
}

sealed interface PreparedShareOriginal {
    data class Ready(
        val uri: String,
        val mimeType: String,
        val label: String,
    ) : PreparedShareOriginal {
        init {
            require(uri.isNotBlank()) { "Share needs a content URI." }
            require(mimeType.isNotBlank()) { "Share needs a MIME type." }
            require(label.isNotBlank()) { "Share needs a label." }
        }
    }

    data object SourceUnavailable : PreparedShareOriginal

    data object CouldNotShare : PreparedShareOriginal
}

sealed interface ShareOriginalOutcome {
    data object Presented : ShareOriginalOutcome

    data object SourceUnavailable : ShareOriginalOutcome

    data object CouldNotShare : ShareOriginalOutcome
}

object ShareOriginalMime {
    const val PDF = "application/pdf"
    const val PHOTO_FALLBACK = "image/jpeg"
    const val SCREENSHOT_FALLBACK = "image/png"
}
