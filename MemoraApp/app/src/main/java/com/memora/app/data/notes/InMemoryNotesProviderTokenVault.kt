package com.memora.app.data.notes

import com.memora.app.domain.notes.NotesProviderSession
import com.memora.app.domain.notes.NotesProviderTokenVault

/** In-memory vault for JVM unit tests. Not used in production DI. */
class InMemoryNotesProviderTokenVault : NotesProviderTokenVault {
    private var session: NotesProviderSession? = null

    override fun readSession(): NotesProviderSession? = session

    override fun writeSession(session: NotesProviderSession) {
        this.session = session
    }

    override fun clearSession() {
        session = null
    }
}
