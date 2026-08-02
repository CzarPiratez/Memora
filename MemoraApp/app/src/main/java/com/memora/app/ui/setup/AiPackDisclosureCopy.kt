package com.memora.app.ui.setup

import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack

/**
 * Honesty copy for embedding-first disclosure / model install / index (E3–E5b1).
 */
object AiPackDisclosureCopy {
    const val ENTRY_LABEL = "About on-device meaning search"

    const val SCREEN_TITLE = "On-device meaning search"

    const val LEAD_BODY =
        "Memora can understand what you already saved — PDF text, OCR, and notes — " +
            "so you can search by meaning, not only exact words. That needs an optional " +
            "on-device meaning model installed on this phone."

    const val SCOPE_TITLE = "What this is"

    const val SCOPE_BODY =
        "The meaning model is stored privately in Memora. It is not your photos, PDFs, " +
            "or notes. This first model is a compact on-device embedder (MediaPipe). " +
            "It is good enough to start meaning search; a larger pack may replace it later. " +
            "Keyword Find saved text buttons remain available."

    const val NETWORK_TITLE = "Network"

    const val NETWORK_BODY =
        "Downloading the meaning model uses the network for model bytes only — never to " +
            "upload your memories. After install, embedding runs on this phone offline."

    const val SIZE_TITLE = "Size on this phone"

    val SIZE_BODY: String =
        "Meaning model download is under about " +
            "${8} MB. Pack-container verify (optional pipeline check) uses about " +
            "${EmbeddingFirstAiPackTrack.PLANNED_DOWNLOAD_SIZE_BYTES} bytes."

    const val LICENSE_TITLE = "License"

    val LICENSE_BODY: String =
        EmbeddingFirstAiPackTrack.PLANNED_LICENSE +
            " The MediaPipe average-word embedder model is subject to Google’s published " +
            "MediaPipe model terms."

    const val STATUS_TITLE = "Status right now"

    const val STATUS_NOT_ACKNOWLEDGED =
        "No disclosure recorded yet. Meaning search is off. " +
            "Keyword search on Welcome still works for text Memora has already saved."

    const val STATUS_NEED_MODEL =
        "Disclosure recorded. Download the on-device meaning model next. " +
            "Meaning search stays off until the model is installed."

    const val STATUS_MODEL_READY =
        "Meaning model is installed on this phone. Build a meaning index from saved " +
            "Asset Memories, then use Find by meaning on Welcome for candidate recall."

    const val STATUS_VERIFYING =
        "Pack verification is in progress. Meaning search stays off until verification finishes."

    const val STATUS_FAILED =
        "Pack verification failed earlier. You can still download the meaning model. " +
            "Your keyword search and original files are unchanged."

    const val STATUS_PACK_ACTIVE_NEED_MODEL =
        "Pack container verified. Download the on-device meaning model to turn on embedding."

    const val ACKNOWLEDGE_LABEL = "I understand these details"

    const val ACTIVATE_LABEL = "Verify pack container (optional)"

    const val DOWNLOAD_MODEL_LABEL = "Download on-device meaning model"

    const val BUILD_INDEX_LABEL = "Build meaning index from memories"

    const val BACK_LABEL = "Back"

    const val FEEDBACK_ACKNOWLEDGED =
        "Saved. Next download the on-device meaning model."

    const val FEEDBACK_ACTIVATED =
        "Pack container verified. Download the meaning model to enable embedding."

    const val FEEDBACK_ALREADY_ACTIVE =
        "Pack container already verified. Download the meaning model if needed."

    const val FEEDBACK_DISCLOSURE_REQUIRED =
        "Acknowledge the details above before continuing."

    const val FEEDBACK_MODEL_INSTALLED =
        "Meaning model installed. You can build a meaning index next."

    const val FEEDBACK_MODEL_ALREADY =
        "Meaning model is already installed on this phone."

    const val FEEDBACK_INDEX_BUILT_PREFIX = "Meaning index updated. Indexed "

    const val FEEDBACK_INDEX_UNAVAILABLE =
        "Meaning model is not ready yet. Download it before building an index."

    fun statusBody(
        installationState: AiPackInstallState,
        disclosureAcknowledged: Boolean,
        modelInstalled: Boolean,
        embeddingAvailable: Boolean,
    ): String = when {
        embeddingAvailable || modelInstalled -> STATUS_MODEL_READY
        installationState == AiPackInstallState.VERIFYING -> STATUS_VERIFYING
        installationState == AiPackInstallState.FAILED_VERIFICATION -> STATUS_FAILED
        installationState == AiPackInstallState.ACTIVE && disclosureAcknowledged ->
            STATUS_PACK_ACTIVE_NEED_MODEL
        disclosureAcknowledged -> STATUS_NEED_MODEL
        else -> STATUS_NOT_ACKNOWLEDGED
    }
}
