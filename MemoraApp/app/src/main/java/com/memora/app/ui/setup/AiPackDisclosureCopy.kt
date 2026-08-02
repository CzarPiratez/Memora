package com.memora.app.ui.setup

import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack

/**
 * Honesty copy for embedding-first AI Pack disclosure (ADR-029 E3).
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
            "PDFs, or notes. Core recall stays on this phone after a pack is installed. " +
            "Today's Find saved text buttons remain keyword matching only."

    const val NETWORK_TITLE = "Network"

    const val NETWORK_BODY =
        "A future pack download would use the network for pack bytes only — never to " +
            "upload your memories. This build does not download a pack. Acknowledging " +
            "below records that you saw the planned size and license; it does not " +
            "install anything or turn on meaning search."

    const val SIZE_TITLE = "Planned size (estimate)"

    val SIZE_BODY: String =
        "Planned download about " +
            "${EmbeddingFirstAiPackTrack.PLANNED_DOWNLOAD_SIZE_BYTES / (1024L * 1024L)} MB. " +
            "Planned free space about " +
            "${EmbeddingFirstAiPackTrack.PLANNED_STORAGE_REQUIREMENT_BYTES / (1024L * 1024L)} MB. " +
            "These are planning estimates until a pack vendor is chosen."

    const val LICENSE_TITLE = "License"

    val LICENSE_BODY: String = EmbeddingFirstAiPackTrack.PLANNED_LICENSE

    const val STATUS_TITLE = "Status right now"

    const val STATUS_NOT_ACKNOWLEDGED =
        "No disclosure recorded yet. Meaning search is off. " +
            "Keyword search on Welcome still works for text Memora has already saved."

    const val STATUS_ACKNOWLEDGED_NOT_INSTALLED =
        "Disclosure recorded on this phone. The pack is not installed. " +
            "Download is not available in this build yet. Meaning search stays off."

    const val STATUS_VERIFYING =
        "Pack verification is in progress. Meaning search stays off until verification finishes."

    const val STATUS_FAILED =
        "Pack verification failed earlier. Meaning search stays off. " +
            "Your keyword search and original files are unchanged."

    const val STATUS_ACTIVE_NOT_CLAIMING_AVAILABLE =
        "A pack install record is active for integrity tracking, but Memora does not " +
            "claim meaning-based search is ready in the product UI until a later measured step."

    const val ACKNOWLEDGE_LABEL = "I understand these details"

    const val BACK_LABEL = "Back"

    const val FEEDBACK_ACKNOWLEDGED =
        "Saved. Download is not available in this build yet. Meaning search stays off."

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
