package com.memora.app.application.notes

import android.app.Activity

/** No-op auth for instrumentation tests that only clear Memora-owned index rows. */
class NoOpOneNoteInteractiveAuth : OneNoteInteractiveAuth {
    override suspend fun connect(activity: Activity): OneNoteAuthOutcome =
        OneNoteAuthOutcome.RegistrationRequired

    override suspend fun disconnect() = Unit

    override suspend fun restoreAccountLabel(): String? = null
}
