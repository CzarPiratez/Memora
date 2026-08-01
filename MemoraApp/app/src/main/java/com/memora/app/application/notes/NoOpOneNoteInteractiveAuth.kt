package com.memora.app.application.notes

import android.app.Activity
import com.memora.app.domain.notes.NotesProviderSession

/** No-op auth for instrumentation tests that only clear Memora-owned index rows. */
class NoOpOneNoteInteractiveAuth : OneNoteInteractiveAuth {
    override suspend fun connect(activity: Activity): OneNoteAuthOutcome =
        OneNoteAuthOutcome.RegistrationRequired

    override suspend fun disconnect() = Unit

    override suspend fun restoreAccountLabel(): String? = null

    override suspend fun ensureSession(): NotesProviderSession? = null
}
