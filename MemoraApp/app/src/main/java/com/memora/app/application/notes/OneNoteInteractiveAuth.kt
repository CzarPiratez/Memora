package com.memora.app.application.notes

import android.app.Activity
import com.memora.app.domain.notes.NotesProviderSession

/**
 * Interactive Microsoft identity for the OneNote-class connector (N2b).
 * Implementations must not invent a connected state without a real token.
 */
interface OneNoteInteractiveAuth {
    suspend fun connect(activity: Activity): OneNoteAuthOutcome

    suspend fun disconnect()

    /**
     * Label only when Memora has a vaulted Graph access token.
     * MSAL account cache alone is not enough to show Connected.
     */
    suspend fun restoreAccountLabel(): String?

    /**
     * Ensures a vaulted access token exists (reuse vault, else silent MSAL refresh).
     * When [forceRefresh] is true, always attempts a silent refresh even if a vaulted
     * token is still present (e.g. after Graph Unauthorized).
     * Returns null when the user must Connect interactively again.
     */
    suspend fun ensureSession(forceRefresh: Boolean = false): NotesProviderSession?
}

sealed interface OneNoteAuthOutcome {
    data class Connected(val session: NotesProviderSession) : OneNoteAuthOutcome

    data object Cancelled : OneNoteAuthOutcome

    data object RegistrationRequired : OneNoteAuthOutcome

    data class Failed(val userMessage: String) : OneNoteAuthOutcome
}
