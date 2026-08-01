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

    suspend fun restoreAccountLabel(): String?
}

sealed interface OneNoteAuthOutcome {
    data class Connected(val session: NotesProviderSession) : OneNoteAuthOutcome

    data object Cancelled : OneNoteAuthOutcome

    data object RegistrationRequired : OneNoteAuthOutcome

    data class Failed(val userMessage: String) : OneNoteAuthOutcome
}
