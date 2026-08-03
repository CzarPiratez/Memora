package com.memora.app.application.intelligence

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveMeaningPdfOpenPageTest {
    @Test
    fun prefers_cue_best_when_clearly_above_cite() {
        val decision = ResolveMeaningPdfOpenPage.decide(
            citedPage = 1,
            scoredPages = listOf(
                ScoredPdfPage(1, 0.20f),
                ScoredPdfPage(3, 0.55f),
            ),
        )
        assertEquals(3, decision.pageNumber)
        assertEquals(MeaningPdfOpenPageBasis.CUE_BEST, decision.basis)
    }

    @Test
    fun keeps_cited_when_cue_not_clearly_better() {
        val decision = ResolveMeaningPdfOpenPage.decide(
            citedPage = 1,
            scoredPages = listOf(
                ScoredPdfPage(1, 0.40f),
                ScoredPdfPage(2, 0.41f),
            ),
        )
        assertEquals(1, decision.pageNumber)
        assertEquals(MeaningPdfOpenPageBasis.CITED, decision.basis)
    }

    @Test
    fun keeps_cited_when_best_is_the_cited_page() {
        val decision = ResolveMeaningPdfOpenPage.decide(
            citedPage = 2,
            scoredPages = listOf(
                ScoredPdfPage(1, 0.10f),
                ScoredPdfPage(2, 0.80f),
            ),
        )
        assertEquals(2, decision.pageNumber)
        assertEquals(MeaningPdfOpenPageBasis.CITED, decision.basis)
    }

    @Test
    fun falls_back_without_scores_or_cite() {
        val decision = ResolveMeaningPdfOpenPage.decide(
            citedPage = null,
            scoredPages = emptyList(),
        )
        assertEquals(1, decision.pageNumber)
        assertEquals(MeaningPdfOpenPageBasis.FALLBACK, decision.basis)
    }

    @Test
    fun uses_cite_when_scoring_unavailable() {
        val decision = ResolveMeaningPdfOpenPage.decide(
            citedPage = 4,
            scoredPages = emptyList(),
        )
        assertEquals(4, decision.pageNumber)
        assertEquals(MeaningPdfOpenPageBasis.CITED, decision.basis)
    }
}
