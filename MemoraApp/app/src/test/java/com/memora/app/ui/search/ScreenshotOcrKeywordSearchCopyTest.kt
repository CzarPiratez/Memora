package com.memora.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenshotOcrKeywordSearchCopyTest {
    @Test
    fun scope_and_why_never_claim_memory_or_photo_ocr() {
        val why = ScreenshotOcrKeywordSearchCopy.whyThisResultBody(
            query = "note",
            screenshotLabel = "Screenshot_memora_note.png",
            excerpt = "…Screenshot note…",
        )
        assertEquals(true, why.contains("keyword matching, not meaning-based recall"))
        assertEquals(true, ScreenshotOcrKeywordSearchCopy.SCOPE_BODY.contains("Ordinary photos"))
        assertEquals(false, ScreenshotOcrKeywordSearchCopy.SCOPE_BODY.contains("PHOTO OCR"))
    }

    @Test
    fun readiness_empty_and_non_empty_are_honest() {
        assertEquals(
            "Nothing is saved for keyword search yet. " +
                "Finish Read text from screenshots in photo setup first.",
            ScreenshotOcrKeywordSearchCopy.readinessBody(0),
        )
        assertEquals(
            "1 screenshot with saved OCR text is ready for keyword search on this phone. " +
                "This is keyword matching, not meaning-based recall.",
            ScreenshotOcrKeywordSearchCopy.readinessBody(1),
        )
    }

    @Test
    fun results_summary_states_cap_without_total_count_claim() {
        val summary = ScreenshotOcrKeywordSearchCopy.resultsSummary(
            query = "note",
            matchCount = 20,
            limitReached = true,
        )
        assertEquals(true, summary.contains("at most 20"))
        assertEquals(false, summary.lowercase().contains("total"))
    }
}
