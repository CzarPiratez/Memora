package com.memora.app.ui.setup

import com.memora.app.domain.notes.OneNoteAuthConfiguration

/**
 * Notes connection honesty copy (N1–N2b).
 * Must not claim notes are indexed or searchable until N3–N5 ship.
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

    const val STATUS_REGISTRATION_REQUIRED =
        "OneNote sign-in is not configured on this build yet. " +
            "A Microsoft app registration (public Android client ID and signature hash) " +
            "is required before Connect can work. No notes are indexed or searchable here."

    const val STATUS_DISCONNECTED =
        "Microsoft app registration is present, but OneNote is not connected on this phone. " +
            "Use Connect OneNote to sign in with Microsoft. No notes are indexed or searchable here yet."

    const val STATUS_CONNECTED_PREFIX =
        "OneNote is connected on this phone for account "

    const val STATUS_CONNECTED_SUFFIX =
        ". Notes are still not indexed or searchable until discovery and extract ship. " +
            "Disconnect clears the Microsoft session on this phone."

    const val CONNECT_LABEL = "Connect OneNote"

    const val DISCONNECT_LABEL = "Disconnect OneNote"

    const val BACK_LABEL = "Back"

    const val FEEDBACK_CONNECTING = "Opening Microsoft sign-in…"

    const val FEEDBACK_DISCONNECTING = "Disconnecting OneNote…"

    const val FEEDBACK_CONNECTED =
        "Connected. Notes are not indexed yet — discovery comes in a later step."

    const val FEEDBACK_DISCONNECTED = "OneNote disconnected on this phone."

    const val FEEDBACK_CANCELLED = "Sign-in cancelled. OneNote stays disconnected."

    const val FEEDBACK_REGISTRATION_REQUIRED =
        "OneNote sign-in is not configured on this build yet."

    fun statusBody(
        registrationConfigured: Boolean,
        connectedAccountLabel: String?,
    ): String = when {
        connectedAccountLabel != null ->
            STATUS_CONNECTED_PREFIX + connectedAccountLabel + STATUS_CONNECTED_SUFFIX
        registrationConfigured -> STATUS_DISCONNECTED
        else -> STATUS_REGISTRATION_REQUIRED
    }

    fun statusBody(configuration: OneNoteAuthConfiguration, connectedAccountLabel: String?): String =
        statusBody(
            registrationConfigured = configuration.isRegistrationConfigured,
            connectedAccountLabel = connectedAccountLabel,
        )
}
