package com.memora.app.ui.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotePageKeywordSearchCopyTest {

    @Test
    fun honestyRejectsSemanticAndCloudClaims() {
        val all = listOf(
            NotePageKeywordSearchCopy.SCREEN_TITLE,
            NotePageKeywordSearchCopy.SCOPE_BODY,
            NotePageKeywordSearchCopy.NOTHING_SAVED_BODY,
            NotePageKeywordSearchCopy.readinessBody(3),
            NotePageKeywordSearchCopy.whyThisResultBody("plan", "Ideas", "…plan…"),
            NotePageKeywordSearchCopy.resultsSummary("plan", 1, limitReached = false),
        ).joinToString(" ").lowercase()

        assertTrue(all.contains("keyword"))
        assertTrue(all.contains("memory evidence") || all.contains("onenote") || all.contains("note"))
        assertFalse(all.contains("meaning-based recall yet") && all.contains("ai understands"))
        assertTrue(all.contains("not meaning-based"))
        assertFalse(all.contains("cloud search"))
        assertFalse(all.contains("memory ranking"))
        assertTrue(NotePageKeywordSearchCopy.readinessBody(3).contains("READY Memory evidence"))
    }

    @Test
    fun openOriginalCopySeparatesNetworkOpenFromOfflineSearch() {
        assertTrue(NotePageKeywordSearchCopy.OPEN_ORIGINAL_NOTE_HINT.contains("network"))
        assertTrue(NotePageKeywordSearchCopy.OPEN_ORIGINAL_NOTE_HINT.contains("does not edit"))
        assertTrue(NotePageKeywordSearchCopy.OPEN_ORIGINAL_NOTE_HINT.contains("Memory evidence"))
        assertTrue(NotePageKeywordSearchCopy.SCOPE_BODY.contains("does not call Microsoft"))
        assertFalse(NotePageKeywordSearchCopy.OPEN_FEEDBACK_OPENING_BODY.contains("preview inside"))
    }
}
