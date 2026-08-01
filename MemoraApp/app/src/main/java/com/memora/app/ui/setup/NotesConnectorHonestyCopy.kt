package com.memora.app.ui.setup

import com.memora.app.domain.notes.OneNoteAuthConfiguration

/**
 * Notes connection + discovery honesty copy (N1–N3).
 * Must not claim note text is searchable until N5.
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
            "Use Connect OneNote to sign in with Microsoft. No notes are searchable here yet."

    const val STATUS_CONNECTED_PREFIX =
        "OneNote is connected on this phone for account "

    const val STATUS_CONNECTED_MID =
        ". Saved note placeholders: "

    const val STATUS_CONNECTED_SUFFIX =
        ". Discover creates placeholders only — note text is not extracted or searchable yet. " +
            "Disconnect clears the Microsoft session on this phone."

    const val CONNECT_LABEL = "Connect OneNote"

    const val DISCONNECT_LABEL = "Disconnect OneNote"

    const val DISCOVER_LABEL = "Discover OneNote pages"

    const val DISCOVER_CONTINUE_LABEL = "Discover more OneNote pages"

    const val BACK_LABEL = "Back"

    const val FEEDBACK_CONNECTING = "Opening Microsoft sign-in…"

    const val FEEDBACK_DISCONNECTING = "Disconnecting OneNote…"

    const val FEEDBACK_CONNECTED =
        "Connected. Discover OneNote pages to save placeholders. Notes are not searchable yet."

    const val FEEDBACK_DISCONNECTED = "OneNote disconnected on this phone."

    const val FEEDBACK_CANCELLED = "Sign-in cancelled. OneNote stays disconnected."

    const val FEEDBACK_REGISTRATION_REQUIRED =
        "OneNote sign-in is not configured on this build yet."

    const val FEEDBACK_DISCOVERING = "Discovering OneNote pages…"

    const val FEEDBACK_ACCESS_REQUIRED =
        "Memora needs a Connect OneNote token on this phone before discovering pages. " +
            "Tap Connect OneNote, finish sign-in, then Discover again."

    const val FEEDBACK_ACCESS_REVOKED =
        "Microsoft access was revoked or expired. Connect OneNote again."

    fun statusBody(
        registrationConfigured: Boolean,
        connectedAccountLabel: String?,
        notePlaceholderCount: Int = 0,
    ): String = when {
        connectedAccountLabel != null ->
            STATUS_CONNECTED_PREFIX +
                connectedAccountLabel +
                STATUS_CONNECTED_MID +
                notePlaceholderCount +
                STATUS_CONNECTED_SUFFIX
        registrationConfigured -> STATUS_DISCONNECTED
        else -> STATUS_REGISTRATION_REQUIRED
    }

    fun statusBody(
        configuration: OneNoteAuthConfiguration,
        connectedAccountLabel: String?,
        notePlaceholderCount: Int = 0,
    ): String = statusBody(
        registrationConfigured = configuration.isRegistrationConfigured,
        connectedAccountLabel = connectedAccountLabel,
        notePlaceholderCount = notePlaceholderCount,
    )

    fun discoveredFeedback(pageCount: Int, totalCount: Int, hasMore: Boolean): String {
        val base = "Saved $pageCount page placeholder(s) this run. " +
            "$totalCount OneNote placeholder(s) total. " +
            "Note text is not extracted or searchable yet."
        return if (hasMore) {
            "$base Tap Discover more to continue."
        } else {
            "$base Discovery is complete for now."
        }
    }
}
