package com.memora.app.data.notes

import com.memora.app.domain.notes.NotesProviderSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryNotesProviderTokenVaultTest {

    @Test
    fun writeReadAndClearSession() {
        val vault = InMemoryNotesProviderTokenVault()
        assertNull(vault.readSession())

        val session = NotesProviderSession(
            providerId = NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE,
            accountId = "home-account-1",
            accountDisplayLabel = "tester@example.com",
            accessToken = "synthetic-token",
            accessTokenExpiresAtEpochMs = 1_700_000_000_000L,
        )
        vault.writeSession(session)
        assertEquals(session, vault.readSession())

        vault.clearSession()
        assertNull(vault.readSession())
    }
}
