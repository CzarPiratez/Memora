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
        assertTrue(all.contains("onenote") || all.contains("note"))
        assertFalse(all.contains("meaning-based recall yet") && all.contains("ai understands"))
        assertTrue(all.contains("not meaning-based"))
        assertFalse(all.contains("cloud search"))
        assertFalse(all.contains("memory ranking"))
    }
}
