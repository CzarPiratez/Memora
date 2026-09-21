package com.memora.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindSourceAvailabilityCopyTest {
    @Test
    fun standing_copy_keeps_the_memory_and_does_not_claim_a_single_cause() {
        val standing = listOf(
            MeaningSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY,
            PdfKeywordSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY,
            PhotoOcrKeywordSearchCopy.SOURCE_UNAVAILABLE_BODY,
            ScreenshotOcrKeywordSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY,
            NotePageKeywordSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY,
        ).joinToString("\n").lowercase()

        assertTrue(standing.contains("memory and why are still here"))
        assertFalse(standing.contains("unavailable."))
        assertFalse(standing.contains("embedding"))
        assertFalse(standing.contains("deleted the memory"))
        assertFalse(standing.contains("openai"))
        assertFalse(standing.contains("it may have been moved"))
        assertFalse(standing.contains("it may have been deleted"))
    }

    @Test
    fun file_surfaces_share_one_list_density_line() {
        val expected = FindSourceAvailabilityCopy.FILE_ORIGINAL_UNREACHABLE
        assertEquals(expected, MeaningSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY)
        assertEquals(expected, PdfKeywordSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY)
        assertEquals(expected, PhotoOcrKeywordSearchCopy.SOURCE_UNAVAILABLE_BODY)
        assertEquals(expected, ScreenshotOcrKeywordSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY)
        assertTrue(expected.length < 80)
    }

    @Test
    fun note_standing_copy_is_list_density_without_a_cause_essay() {
        val expected = FindSourceAvailabilityCopy.NOTE_ORIGINAL_UNREACHABLE
        assertEquals(
            expected,
            NotePageKeywordSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY,
        )
        val body = expected.lowercase()
        assertTrue(body.contains("that note cannot be opened"))
        assertTrue(body.contains("memory and why are still here"))
        assertFalse(body.contains("unavailable."))
        assertFalse(body.contains("renew"))
        assertTrue(expected.length < 80)
    }

    @Test
    fun try_open_is_the_retry_label() {
        assertTrue(MeaningSearchCopy.OPEN_ORIGINAL.tryOpenLabel.contains("Try Open"))
        assertTrue(PdfKeywordSearchCopy.OPEN_ORIGINAL.tryOpenLabel.contains("Try Open"))
        assertTrue(PhotoOcrKeywordSearchCopy.OPEN_ORIGINAL.tryOpenLabel.contains("Try Open"))
        assertTrue(ScreenshotOcrKeywordSearchCopy.OPEN_ORIGINAL.tryOpenLabel.contains("Try Open"))
        assertTrue(NotePageKeywordSearchCopy.OPEN_ORIGINAL.tryOpenLabel.contains("Try Open"))
    }
}
