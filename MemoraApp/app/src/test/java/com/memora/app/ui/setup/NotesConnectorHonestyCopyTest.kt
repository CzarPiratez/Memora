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
            NotesConnectorHonestyCopy.STATUS_BODY,
        ).joinToString(" ").lowercase()

        assertTrue(all.contains("onenote"))
        assertTrue(all.contains("microsoft"))
        assertTrue(all.contains("network"))
        assertTrue(NotesConnectorHonestyCopy.SCOPE_BODY.contains("not a Memora account"))
        assertTrue(NotesConnectorHonestyCopy.STATUS_BODY.contains("not available"))
        assertTrue(NotesConnectorHonestyCopy.STATUS_BODY.contains("No notes are indexed"))

        assertFalse(all.contains("connected"))
        assertFalse(all.contains("all notes on"))
        assertFalse(all.contains("memory ranking"))
        assertFalse(all.contains("connect onenote"))
    }
}
