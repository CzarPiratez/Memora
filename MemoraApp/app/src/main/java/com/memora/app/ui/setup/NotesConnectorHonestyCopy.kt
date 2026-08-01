package com.memora.app.ui.setup

/**
 * Plain-language Notes honesty copy for ADR-003 / N1.
 *
 * Explains the OneNote-class connector path. Must not claim notes are indexed,
 * connected, or searchable yet. No MSAL or Graph calls belong in this layer.
 */
object NotesConnectorHonestyCopy {
    const val ENTRY_LABEL = "About Notes indexing"

    const val SCREEN_TITLE = "Notes indexing"

    const val LEAD_BODY =
        "Memora will index notes only through an approved connector. " +
            "The first target is Microsoft OneNote. Memora cannot read other note apps' " +
            "private data on Android."

    const val SCOPE_TITLE = "What this is"

    const val SCOPE_BODY =
        "Connecting OneNote uses your Microsoft account for OneNote access only. " +
            "That is not a Memora account, and it does not sync Memora's index to Microsoft. " +
            "Access stays read-only — Memora will not change or delete your notes."

    const val NETWORK_TITLE = "Network"

    const val NETWORK_BODY =
        "Signing in and reading OneNote pages needs a network connection. " +
            "That is source access, not Memora cloud sync. After note text is saved on " +
            "this phone, search over those saved facts can work offline."

    const val STATUS_TITLE = "Status right now"

    const val STATUS_BODY =
        "OneNote connection is not available in this build yet. " +
            "No notes are indexed or searchable here. Photo, screenshot, and PDF " +
            "keyword search remain the interim recall paths."

    const val BACK_LABEL = "Back"
}
