package com.memora.app.ui.setup

import com.memora.app.application.intelligence.MeaningIndexBatchLimits
import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.MediaPipeUniversalSentenceEncoderSpec

/**
 * Honesty copy for embedding-first disclosure / model install / index (E3–E4b).
 */
object AiPackDisclosureCopy {
    const val ENTRY_LABEL = "About on-device meaning search"

    const val SCREEN_TITLE = "On-device meaning search"

    const val LEAD_BODY =
        "UNFYND can understand what you already saved — PDF text, OCR, and notes — " +
            "so you can search by meaning, not only exact words. That needs an optional " +
            "on-device meaning model installed on this phone."

    const val SCOPE_TITLE = "What this is"

    const val SCOPE_BODY =
        "The meaning model is stored privately in UNFYND. It is not your photos, PDFs, " +
            "or notes. This model is MediaPipe’s Universal Sentence Encoder — a stronger " +
            "on-device semantic embedder than the earlier compact average-word model. " +
            "Meaning search stays a candidate path until measured on your device class. " +
            "Keyword Find saved text buttons remain available."

    const val NETWORK_TITLE = "Network"

    const val NETWORK_BODY =
        "Downloading the meaning model uses the network for model bytes only — never to " +
            "upload your memories. After install, embedding runs on this phone offline."

    const val SIZE_TITLE = "Size on this phone"

    val SIZE_BODY: String =
        "Meaning model download is under about " +
            "${MediaPipeUniversalSentenceEncoderSpec.DISCLOSED_SIZE_MB_CEILING} MB. " +
            "Pack-container verify (optional pipeline check) uses about " +
            "${EmbeddingFirstAiPackTrack.PLANNED_DOWNLOAD_SIZE_BYTES} bytes. " +
            "After upgrading the model, rebuild the meaning index."

    const val LICENSE_TITLE = "License"

    val LICENSE_BODY: String =
        EmbeddingFirstAiPackTrack.PLANNED_LICENSE +
            " The MediaPipe Universal Sentence Encoder model is subject to Google’s " +
            "published MediaPipe model terms."

    const val STATUS_TITLE = "Status right now"

    const val STATUS_NOT_ACKNOWLEDGED =
        "No disclosure recorded yet. Meaning search is off. " +
            "Keyword search on Welcome still works for text UNFYND has already saved."

    const val STATUS_NEED_MODEL =
        "Disclosure recorded. Download the on-device meaning model next. " +
            "Meaning search stays off until the model is installed."

    val STATUS_MODEL_READY: String =
        "Meaning model is installed on this phone. Build a meaning index from saved " +
            "Asset Memories (up to ${MeaningIndexBatchLimits.MAX_MEMORIES_PER_TAP} " +
            "memories per tap — tap again if more remain), then use Find by meaning " +
            "on Welcome for candidate recall."

    const val STATUS_VERIFYING =
        "Pack verification is in progress. Meaning search stays off until verification finishes."

    const val STATUS_FAILED =
        "Pack verification failed earlier. You can still download the meaning model. " +
            "Your keyword search and original files are unchanged."

    const val STATUS_PACK_ACTIVE_NEED_MODEL =
        "Pack container verified. Download the on-device meaning model to turn on embedding."

    const val INDEX_BATCH_TITLE = "Building the meaning index"

    val INDEX_BATCH_BODY: String =
        "Each Build indexes up to ${MeaningIndexBatchLimits.MAX_MEMORIES_PER_TAP} " +
            "READY memories this tap, plus PDF pages for those PDFs and OCR evidence " +
            "for photos/screenshots (capped per asset). If more READY memories remain, " +
            "tap Build again. This is not a permanent library ceiling."

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
        "Meaning model installed. Rebuild the meaning index so rankings use this model."

    const val FEEDBACK_MODEL_ALREADY =
        "Meaning model is already installed on this phone."

    const val FEEDBACK_INDEX_BUILT_PREFIX = "Meaning index updated. Indexed "

    const val FEEDBACK_INDEX_UNAVAILABLE =
        "Meaning model is not ready yet. Download it before building an index."

    const val FEEDBACK_INDEX_EMPTY =
        "No READY memories to index yet. Build Asset Memory first, then return here."

    const val PROGRESS_PREPARING = "Preparing meaning index…"

    fun progressSummaries(processed: Int, total: Int): String =
        "Indexing summaries $processed of $total…"

    fun progressPages(processed: Int, total: Int): String =
        "Indexing PDF pages $processed of $total…"

    fun progressOcrEvidence(processed: Int, total: Int): String =
        "Indexing photo/screenshot OCR evidence $processed of $total…"

    fun remainingBatchHint(remaining: Int): String =
        "$remaining READY left — tap Build again for the next batch."

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
