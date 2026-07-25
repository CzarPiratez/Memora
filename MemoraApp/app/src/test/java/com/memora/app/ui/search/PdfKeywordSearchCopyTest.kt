package com.memora.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfKeywordSearchCopyTest {
    @Test
    fun copy_states_keyword_not_meaning_and_avoids_jargon() {
        val copy = listOf(
            PdfKeywordSearchCopy.SCREEN_TITLE,
            PdfKeywordSearchCopy.SCOPE_BODY,
            PdfKeywordSearchCopy.EMPTY_QUERY_BODY,
            PdfKeywordSearchCopy.NO_MATCHES_BODY,
            PdfKeywordSearchCopy.RESULTS_HINT,
            PdfKeywordSearchCopy.resultsSummary(matchCount = 2, limitReached = false),
            PdfKeywordSearchCopy.resultsSummary(
                matchCount = PdfKeywordSearchCopy.MAX_LISTED_MATCHES,
                limitReached = true,
            ),
            PdfKeywordSearchCopy.WHY_THIS_RESULT_LABEL,
            PdfKeywordSearchCopy.whyThisResultBody(
                query = "meet mira",
                documentLabel = "memora-persist-fixture.pdf",
                pageNumber = 2,
                excerpt = "…meet mira tomorrow…",
            ),
        ).joinToString("\n").lowercase()

        assertTrue(copy.contains("keyword"))
        assertTrue(copy.contains("not meaning-based"))
        assertTrue(copy.contains("memora-persist-fixture.pdf"))
        assertFalse(copy.contains("sqlcipher"))
        assertFalse(copy.contains("binder"))
        assertFalse(copy.contains("embedding"))
        assertFalse(copy.contains("vector"))
        assertFalse(copy.contains("openai"))
        assertFalse(copy.contains("confidence"))
        assertFalse(copy.contains("fingerprint"))
        assertFalse(copy.contains("schema"))
    }

    @Test
    fun why_this_result_cites_query_document_page_and_excerpt() {
        assertEquals(
            "Memora matched \"meet mira\" in saved PDF page text from " +
                "\"memora-persist-fixture.pdf\" on this phone (Page 1). " +
                "Matching evidence: Café memory: meet Mira at 10:30…. " +
                "This is keyword matching, not meaning-based recall.",
            PdfKeywordSearchCopy.whyThisResultBody(
                query = "meet mira",
                documentLabel = "memora-persist-fixture.pdf",
                pageNumber = 1,
                excerpt = "Café memory: meet Mira at 10:30…",
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun why_this_result_rejects_blank_document_label() {
        PdfKeywordSearchCopy.whyThisResultBody(
            query = "meet mira",
            documentLabel = " ",
            pageNumber = 1,
            excerpt = "meet mira",
        )
    }

    @Test
    fun results_summary_states_count_without_cap_when_under_limit() {
        assertEquals(
            "Showing 2 matches. " + PdfKeywordSearchCopy.RESULTS_HINT,
            PdfKeywordSearchCopy.resultsSummary(matchCount = 2, limitReached = false),
        )
        assertEquals(
            "Showing 1 match. " + PdfKeywordSearchCopy.RESULTS_HINT,
            PdfKeywordSearchCopy.resultsSummary(matchCount = 1, limitReached = false),
        )
    }

    @Test
    fun results_summary_discloses_cap_when_limit_reached() {
        val summary = PdfKeywordSearchCopy.resultsSummary(
            matchCount = PdfKeywordSearchCopy.MAX_LISTED_MATCHES,
            limitReached = true,
        )
        assertEquals(
            "Showing 20 matches. Memora lists at most 20 matches for now; " +
                "more saved pages may also contain these words. " +
                PdfKeywordSearchCopy.RESULTS_HINT,
            summary,
        )
        assertFalse(summary.lowercase().contains("confidence"))
        assertFalse(summary.lowercase().contains("semantic memory"))
        assertFalse(summary.lowercase().contains("ai "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun results_summary_rejects_zero_matches() {
        PdfKeywordSearchCopy.resultsSummary(matchCount = 0, limitReached = false)
    }
}
