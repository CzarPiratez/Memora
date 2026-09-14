package com.memora.app.application.handoff

/**
 * Identity needed to hand a stored original to another app on this phone.
 *
 * One request shape for both handoffs the person can tap: the share sheet
 * (ADR-053) and Open in another app. Neither is Act — UNFYND grants a read of
 * a file the person already has, and never copies, uploads, or edits it.
 * Notes have no local file URI in this slice.
 */
sealed interface OriginalHandoffRequest {
    val sourceId: String
    val sourceAssetKey: String
    val label: String

    data class Pdf(
        override val sourceId: String,
        override val sourceAssetKey: String,
        override val label: String,
    ) : OriginalHandoffRequest {
        init {
            require(sourceId.isNotBlank() && sourceAssetKey.isNotBlank() && label.isNotBlank())
        }
    }

    data class Photo(
        override val sourceId: String,
        override val sourceAssetKey: String,
        override val label: String,
    ) : OriginalHandoffRequest {
        init {
            require(sourceId.isNotBlank() && sourceAssetKey.isNotBlank() && label.isNotBlank())
        }
    }

    data class Screenshot(
        override val sourceId: String,
        override val sourceAssetKey: String,
        override val label: String,
    ) : OriginalHandoffRequest {
        init {
            require(sourceId.isNotBlank() && sourceAssetKey.isNotBlank() && label.isNotBlank())
        }
    }
}

sealed interface PreparedOriginalHandoff {
    data class Ready(
        val uri: String,
        val mimeType: String,
        val label: String,
    ) : PreparedOriginalHandoff {
        init {
            require(uri.isNotBlank()) { "A handoff needs a content URI." }
            require(mimeType.isNotBlank()) { "A handoff needs a MIME type." }
            require(label.isNotBlank()) { "A handoff needs a label." }
        }
    }

    data object SourceUnavailable : PreparedOriginalHandoff

    data object CouldNotHandOff : PreparedOriginalHandoff
}

object OriginalHandoffMime {
    const val PDF = "application/pdf"
    const val PHOTO_FALLBACK = "image/jpeg"
    const val SCREENSHOT_FALLBACK = "image/png"
}

/** Outcome of the share-sheet handoff. */
sealed interface ShareOriginalOutcome {
    data object Presented : ShareOriginalOutcome

    data object SourceUnavailable : ShareOriginalOutcome

    data object CouldNotShare : ShareOriginalOutcome
}

/**
 * Outcome of opening the whole original in another app.
 *
 * [NoAppAvailable] is separate from [CouldNotOpen] because the person can fix
 * it (install a reader) and UNFYND must not blame the file for it.
 */
sealed interface OpenOriginalOutcome {
    data object Opened : OpenOriginalOutcome

    data object NoAppAvailable : OpenOriginalOutcome

    data object SourceUnavailable : OpenOriginalOutcome

    data object CouldNotOpen : OpenOriginalOutcome
}
