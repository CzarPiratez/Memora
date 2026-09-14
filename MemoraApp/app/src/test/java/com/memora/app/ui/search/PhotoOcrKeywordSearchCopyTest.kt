package com.memora.app.ui.search

import com.memora.app.application.memory.CanonicalRecallTestFixtures
import com.memora.app.domain.asset.AssetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoOcrKeywordSearchCopyTest {
    @Test
    fun scope_and_why_stay_keyword_honest_not_meaning_or_screenshot() {
        val why = PhotoOcrKeywordSearchCopy.whyThisResultBody(
            query = "total",
            recall = CanonicalRecallTestFixtures.keywordRecall(
                assetType = AssetType.PHOTO,
                label = "receipt.jpg",
                excerpt = "…Total 42…",
                pageNumber = null,
            ),
        )
        assertTrue(why.contains("exact words"))
        assertTrue(why.contains("You asked about \"total\""))
        assertTrue(why.contains("That word is in this file's saved text"))
        assertFalse(why.contains("Matched photo:"))
        assertFalse(why.contains("receipt.jpg"))
        assertFalse(why.contains("…Total 42…"))
        assertTrue(PhotoOcrKeywordSearchCopy.SCOPE_BODY.contains("Screenshots are not included"))
        assertTrue(PhotoOcrKeywordSearchCopy.SCOPE_BODY.contains("Memory evidence"))
        assertFalse(PhotoOcrKeywordSearchCopy.SCOPE_BODY.contains("SCREENSHOT OCR"))
    }

    @Test
    fun readiness_empty_and_non_empty_are_honest() {
        assertEquals(
            "Nothing is ready for photo keyword search yet. " +
                "Finish Read text from photos and Memory assembly in photo setup first.",
            PhotoOcrKeywordSearchCopy.readinessBody(0),
        )
        assertEquals(
            "1 photo with READY Memory evidence is ready for exact-word search on this phone.",
            PhotoOcrKeywordSearchCopy.readinessBody(1),
        )
    }

    @Test
    fun cappedResultsAreExplicit() {
        assertTrue(
            PhotoOcrKeywordSearchCopy.resultsSummary("total", 20, true)
                .contains("at most 20"),
        )
    }

    @Test
    fun preview_copy_stays_memory_evidence_honest() {
        assertTrue(PhotoOcrKeywordSearchCopy.PREVIEW_SCOPE_BODY.contains("Memory evidence"))
    }
}
