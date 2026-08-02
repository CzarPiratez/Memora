package com.memora.app.ui.setup

import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack

/**
 * Honesty copy for embedding-first AI Pack disclosure / offline container (E3–E4a).
 *
 * Never claims Local Intelligence AVAILABLE or that meaning search works today.
 */
object AiPackDisclosureCopy {
    const val ENTRY_LABEL = "About on-device meaning search"

    const val SCREEN_TITLE = "On-device meaning search"

    const val LEAD_BODY =
        "Memora can later understand what you already saved — PDF text, OCR, and notes — " +
            "so you can search by meaning, not only exact words. That needs an optional " +
            "on-device AI Pack installed on this phone."

    const val SCOPE_TITLE = "What this is"

    const val SCOPE_BODY =
        "An AI Pack is model data stored privately in Memora. It is not your photos, " +
            "PDFs, or notes. This build can verify and store a small pack container for " +
            "Memora's install pipeline. That is not meaning-based search yet. " +
            "Today's Find saved text buttons remain keyword matching only."

    const val NETWORK_TITLE = "Network"

    const val NETWORK_BODY =
        "Verifying the pack container in this build stays on this phone — no download. " +
            "A future larger embedding model pack may use the network for pack bytes only, " +
            "never to upload your memories."

    const val SIZE_TITLE = "Size on this phone"

    val SIZE_BODY: String =
        "Pack container about " +
            "${EmbeddingFirstAiPackTrack.PLANNED_DOWNLOAD_SIZE_BYTES} bytes to verify, " +
            "about ${EmbeddingFirstAiPackTrack.PLANNED_STORAGE_REQUIREMENT_BYTES} bytes " +
            "of private storage. A larger embedding model may replace this container later."

    const val LICENSE_TITLE = "License"

    val LICENSE_BODY: String = EmbeddingFirstAiPackTrack.PLANNED_LICENSE

    const val STATUS_TITLE = "Status right now"

    const val STATUS_NOT_ACKNOWLEDGED =
        "No disclosure recorded yet. Meaning search is off. " +
            "Keyword search on Welcome still works for text Memora has already saved."

    const val STATUS_ACKNOWLEDGED_NOT_INSTALLED =
        "Disclosure recorded. You can verify and store the pack container next. " +
            "Meaning search stays off."

    const val STATUS_VERIFYING =
        "Pack verification is in progress. Meaning search stays off until verification finishes."

    const val STATUS_FAILED =
        "Pack verification failed earlier. Meaning search stays off. " +
            "Your keyword search and original files are unchanged."

    const val STATUS_ACTIVE_NOT_CLAIMING_AVAILABLE =
        "Pack container verified and stored on this phone. Memora does not claim " +
            "meaning-based search is ready until a later measured embedding step."

    const val ACKNOWLEDGE_LABEL = "I understand these details"

    const val ACTIVATE_LABEL = "Verify and store pack container"

    const val BACK_LABEL = "Back"

    const val FEEDBACK_ACKNOWLEDGED =
        "Saved. Next you can verify and store the pack container. Meaning search stays off."

    const val FEEDBACK_ACTIVATED =
        "Pack container verified and stored. Meaning search stays off."

    const val FEEDBACK_ALREADY_ACTIVE =
        "Pack container is already verified on this phone. Meaning search stays off."

    const val FEEDBACK_DISCLOSURE_REQUIRED =
        "Acknowledge the details above before verifying the pack container."

    fun statusBody(
        installationState: AiPackInstallState,
        disclosureAcknowledged: Boolean,
    ): String = when {
        installationState == AiPackInstallState.ACTIVE ->
            STATUS_ACTIVE_NOT_CLAIMING_AVAILABLE
        installationState == AiPackInstallState.VERIFYING -> STATUS_VERIFYING
        installationState == AiPackInstallState.FAILED_VERIFICATION -> STATUS_FAILED
        disclosureAcknowledged -> STATUS_ACKNOWLEDGED_NOT_INSTALLED
        else -> STATUS_NOT_ACKNOWLEDGED
    }
}
