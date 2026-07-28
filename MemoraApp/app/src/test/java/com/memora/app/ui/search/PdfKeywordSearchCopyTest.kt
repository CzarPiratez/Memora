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
            PdfKeywordSearchCopy.SEARCHING_BODY,
            PdfKeywordSearchCopy.CLEAR_QUERY_LABEL,
            PdfKeywordSearchCopy.CANCEL_SEARCH_LABEL,
            PdfKeywordSearchCopy.NO_MATCHES_BODY,
            PdfKeywordSearchCopy.noMatchesBody("meet mira"),
            PdfKeywordSearchCopy.NOTHING_SAVED_BODY,
            PdfKeywordSearchCopy.readinessBody(pageCount = 0, documentCount = 0),
            PdfKeywordSearchCopy.readinessBody(pageCount = 3, documentCount = 2),
            PdfKeywordSearchCopy.RESULTS_HINT,
            PdfKeywordSearchCopy.resultsSummary(
                query = "meet mira",
                matchCount = 2,
                limitReached = false,
            ),
            PdfKeywordSearchCopy.resultsSummary(
                query = "meet mira",
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
            PdfKeywordSearchCopy.OPEN_ORIGINAL_PDF_LABEL,
            PdfKeywordSearchCopy.OPEN_ORIGINAL_PDF_HINT,
            PdfKeywordSearchCopy.OPEN_FEEDBACK_OPENING_BODY,
            PdfKeywordSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY,
            PdfKeywordSearchCopy.OPEN_FEEDBACK_COULD_NOT_OPEN_BODY,
            PdfKeywordSearchCopy.PREVIEW_TITLE,
            PdfKeywordSearchCopy.PREVIEW_SCOPE_BODY,
            PdfKeywordSearchCopy.previewPageCaption(pageNumber = 2, pageCount = 5),
            PdfKeywordSearchCopy.previewImageContentDescription(
                documentLabel = "memora-open-2page.pdf",
                pageNumber = 2,
                pageCount = 5,
            ),
        ).joinToString("\n").lowercase()

        assertTrue(copy.contains("keyword"))
        assertTrue(copy.contains("not meaning-based"))
        assertTrue(copy.contains("results for \"meet mira\""))
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
    fun results_summary_names_submitted_query_and_count() {
        assertEquals(
            "Results for \"meet mira\". Showing 2 matches. " +
                PdfKeywordSearchCopy.RESULTS_HINT,
            PdfKeywordSearchCopy.resultsSummary(
                query = "meet mira",
                matchCount = 2,
                limitReached = false,
            ),
        )
        assertEquals(
            "Results for \"cafe\". Showing 1 match. " + PdfKeywordSearchCopy.RESULTS_HINT,
            PdfKeywordSearchCopy.resultsSummary(
                query = "cafe",
                matchCount = 1,
                limitReached = false,
            ),
        )
    }

    @Test
    fun results_summary_discloses_cap_when_limit_reached() {
        val summary = PdfKeywordSearchCopy.resultsSummary(
            query = "meet mira",
            matchCount = PdfKeywordSearchCopy.MAX_LISTED_MATCHES,
            limitReached = true,
        )
        assertEquals(
            "Results for \"meet mira\". Showing 20 matches. " +
                "Memora lists at most 20 matches for now; " +
                "more saved pages may also contain these words. " +
                PdfKeywordSearchCopy.RESULTS_HINT,
            summary,
        )
        assertFalse(summary.lowercase().contains("confidence"))
        assertFalse(summary.lowercase().contains("semantic memory"))
        assertFalse(summary.lowercase().contains("ai "))
    }

    @Test
    fun no_matches_body_names_submitted_query() {
        assertEquals(
            "No saved PDF page text on this phone matched \"zzz\". " +
                "Try different words, or finish Local PDF reading for a document first.",
            PdfKeywordSearchCopy.noMatchesBody("zzz"),
        )
    }

    @Test
    fun nothing_saved_copy_does_not_claim_a_keyword_miss() {
        val body = PdfKeywordSearchCopy.NOTHING_SAVED_BODY.lowercase()
        assertTrue(body.contains("nothing is saved"))
        assertTrue(body.contains("local pdf reading"))
        assertTrue(body.contains("keyword matching"))
        assertFalse(body.contains("matched \""))
        assertFalse(body.contains("try different words"))
        assertFalse(body.contains("confidence"))
        assertFalse(body.contains("ai "))
    }

    @Test
    fun readiness_body_reports_empty_and_non_empty_honestly() {
        assertEquals(
            "Nothing is saved for keyword search yet. " +
                "Finish Local PDF reading for a connected folder first.",
            PdfKeywordSearchCopy.readinessBody(pageCount = 0, documentCount = 0),
        )
        assertEquals(
            "1 saved page from 1 PDF is ready for keyword search on this phone. " +
                "This is keyword matching, not meaning-based recall.",
            PdfKeywordSearchCopy.readinessBody(pageCount = 1, documentCount = 1),
        )
        assertEquals(
            "3 saved pages from 2 PDFs are ready for keyword search on this phone. " +
                "This is keyword matching, not meaning-based recall.",
            PdfKeywordSearchCopy.readinessBody(pageCount = 3, documentCount = 2),
        )
    }

    @Test
    fun preview_image_description_names_document_and_page() {
        assertEquals(
            "Read-only preview of \"memora-open-2page.pdf\", Page 2 of 5.",
            PdfKeywordSearchCopy.previewImageContentDescription(
                documentLabel = "memora-open-2page.pdf",
                pageNumber = 2,
                pageCount = 5,
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun preview_image_description_rejects_blank_document_label() {
        PdfKeywordSearchCopy.previewImageContentDescription(
            documentLabel = " ",
            pageNumber = 1,
            pageCount = 1,
        )
    }

    @Test
    fun searching_copy_is_calm_and_local_only() {
        val body = PdfKeywordSearchCopy.SEARCHING_BODY.lowercase()
        assertTrue(body.contains("searching saved pdf text"))
        assertTrue(body.contains("on this phone"))
        assertFalse(body.contains("ai "))
        assertFalse(body.contains("cloud"))
        assertFalse(body.contains("meaning"))
        assertFalse(body.contains("confidence"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun results_summary_rejects_zero_matches() {
        PdfKeywordSearchCopy.resultsSummary(
            query = "meet mira",
            matchCount = 0,
            limitReached = false,
        )
    }
}
