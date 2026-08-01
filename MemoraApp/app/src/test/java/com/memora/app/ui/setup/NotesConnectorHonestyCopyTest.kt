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
            NotesConnectorHonestyCopy.STATUS_CONNECTED_MID,
            NotesConnectorHonestyCopy.STATUS_CONNECTED_SUFFIX,
            NotesConnectorHonestyCopy.CONNECT_LABEL,
            NotesConnectorHonestyCopy.DISCOVER_LABEL,
            NotesConnectorHonestyCopy.EXTRACT_LABEL,
            NotesConnectorHonestyCopy.FEEDBACK_CONNECTED,
        ).joinToString(" ").lowercase()

        assertTrue(all.contains("onenote"))
        assertTrue(all.contains("microsoft"))
        assertTrue(all.contains("network"))
        assertTrue(NotesConnectorHonestyCopy.SCOPE_BODY.contains("not a Memora account"))
        assertTrue(NotesConnectorHonestyCopy.STATUS_CONNECTED_SUFFIX.contains("Find saved note text"))
        assertTrue(NotesConnectorHonestyCopy.STATUS_CONNECTED_SUFFIX.contains("keyword"))
        assertTrue(NotesConnectorHonestyCopy.FEEDBACK_CONNECTED.contains("Find saved note text"))

        assertFalse(all.contains("all notes on"))
        assertFalse(all.contains("memory ranking"))
        assertFalse(all.contains("meaning-based recall yet") && !all.contains("not meaning"))
    }

    @Test
    fun discoveredFeedbackStaysPlaceholderHonest() {
        val done = NotesConnectorHonestyCopy.discoveredFeedback(2, 5, hasMore = false)
        assertTrue(done.contains("2 page placeholder"))
        assertTrue(done.contains("5 OneNote placeholder"))
        assertTrue(done.contains("Extract"))

        val more = NotesConnectorHonestyCopy.discoveredFeedback(2, 5, hasMore = true)
        assertTrue(more.contains("Discover more"))
    }

    @Test
    fun extractedFeedbackPointsToFindSavedNoteText() {
        val done = NotesConnectorHonestyCopy.extractedFeedback(3, 3)
        assertTrue(done.contains("3 page"))
        assertTrue(done.contains("Find saved note text"))
        val pending = NotesConnectorHonestyCopy.extractedFeedback(1, 4)
        assertTrue(pending.contains("1 of 4"))
        assertTrue(pending.contains("Find saved note text"))
    }
}
