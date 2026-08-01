package com.memora.app.ui.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesConnectorHonestyCopyTest {

    @Test
    fun honestyCopyNamesOneNoteAndRejectsOverclaims() {
        val all = listOf(
            NotesConnectorHonestyCopy.ENTRY_LABEL,
            NotesConnectorHonestyCopy.SCREEN_TITLE,
            NotesConnectorHonestyCopy.LEAD_BODY,
            NotesConnectorHonestyCopy.SCOPE_BODY,
            NotesConnectorHonestyCopy.NETWORK_BODY,
            NotesConnectorHonestyCopy.STATUS_REGISTRATION_REQUIRED,
            NotesConnectorHonestyCopy.STATUS_DISCONNECTED,
            NotesConnectorHonestyCopy.STATUS_CONNECTED_PREFIX,
            NotesConnectorHonestyCopy.STATUS_CONNECTED_SUFFIX,
            NotesConnectorHonestyCopy.CONNECT_LABEL,
            NotesConnectorHonestyCopy.FEEDBACK_CONNECTED,
        ).joinToString(" ").lowercase()

        assertTrue(all.contains("onenote"))
        assertTrue(all.contains("microsoft"))
        assertTrue(all.contains("network"))
        assertTrue(NotesConnectorHonestyCopy.SCOPE_BODY.contains("not a Memora account"))
        assertTrue(
            NotesConnectorHonestyCopy.STATUS_REGISTRATION_REQUIRED.contains("not configured"),
        )
        assertTrue(
            NotesConnectorHonestyCopy.STATUS_DISCONNECTED.contains("Connect OneNote"),
        )
        assertTrue(
            NotesConnectorHonestyCopy.STATUS_CONNECTED_SUFFIX.contains("not indexed"),
        )
        assertTrue(NotesConnectorHonestyCopy.FEEDBACK_CONNECTED.contains("not indexed"))

        assertFalse(all.contains("all notes on"))
        assertFalse(all.contains("memory ranking"))
    }

    @Test
    fun statusBodyTracksRegistrationAndSessionWithoutClaimingSearch() {
        val registration = NotesConnectorHonestyCopy.statusBody(
            registrationConfigured = false,
            connectedAccountLabel = null,
        )
        assertTrue(registration.contains("not configured"))

        val disconnected = NotesConnectorHonestyCopy.statusBody(
            registrationConfigured = true,
            connectedAccountLabel = null,
        )
        assertTrue(disconnected.contains("not connected"))
        assertTrue(disconnected.contains("Connect OneNote"))

        val connected = NotesConnectorHonestyCopy.statusBody(
            registrationConfigured = true,
            connectedAccountLabel = "user@example.com",
        )
        assertTrue(connected.contains("user@example.com"))
        assertTrue(connected.contains("not indexed"))
    }
}
