package com.memora.app.ui.search

import com.memora.app.application.memory.CanonicalRecallTestFixtures
import com.memora.app.domain.asset.AssetType
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenshotOcrKeywordSearchCopyTest {
    @Test
    fun scope_and_why_stay_keyword_honest_not_meaning_or_photo() {
        val why = ScreenshotOcrKeywordSearchCopy.whyThisResultBody(
            query = "note",
            recall = CanonicalRecallTestFixtures.keywordRecall(
                assetType = AssetType.SCREENSHOT,
                label = "Screenshot_memora_note.png",
                excerpt = "…Screenshot note…",
                pageNumber = null,
            ),
        )
        assertEquals(true, why.contains("exact words"))
        assertEquals(true, why.contains("You asked about \"note\""))
        assertEquals(true, why.contains("That word is in this file's saved text"))
        assertEquals(false, why.contains("Matched screenshot:"))
        assertEquals(false, why.contains("Screenshot_memora_note.png"))
        assertEquals(true, ScreenshotOcrKeywordSearchCopy.SCOPE_BODY.contains("Ordinary photos"))
        assertEquals(true, ScreenshotOcrKeywordSearchCopy.SCOPE_BODY.contains("Memory evidence"))
        assertEquals(false, ScreenshotOcrKeywordSearchCopy.SCOPE_BODY.contains("PHOTO OCR"))
    }

    @Test
    fun readiness_empty_and_non_empty_are_honest() {
        assertEquals(
            "Nothing is ready for screenshot keyword search yet. " +
                "Finish Read text from screenshots and Memory assembly in photo setup first.",
            ScreenshotOcrKeywordSearchCopy.readinessBody(0),
        )
        assertEquals(
            "1 screenshot with READY Memory evidence is ready for exact-word search on this phone.",
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

    @Test
    fun open_and_preview_copy_stay_read_only_and_keyword_honest() {
        assertEquals(
            true,
            ScreenshotOcrKeywordSearchCopy.OPEN_ORIGINAL_SCREENSHOT_HINT.contains("read-only"),
        )
        assertEquals(
            true,
            ScreenshotOcrKeywordSearchCopy.PREVIEW_SCOPE_BODY.contains("Memory evidence"),
        )
        assertEquals(
            "Read-only preview of \"Screenshot_memora_note.png\".",
            ScreenshotOcrKeywordSearchCopy.previewImageContentDescription(
                "Screenshot_memora_note.png",
            ),
        )
    }
}
