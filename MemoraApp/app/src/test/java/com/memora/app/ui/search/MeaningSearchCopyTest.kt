package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchReadiness
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.CorpusCompletenessCounts
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningSearchCopyTest {
    @Test
    fun scope_and_readiness_describe_on_device_meaning_search() {
        val ready = MeaningSearchCopy.readinessBody(
            MeaningSearchReadiness.Ready(
                model = ModelVersionIdentity("m", "1"),
                indexedCount = 3,
                memoriesReadyCount = 3,
                corpusCompleteness = CorpusCompletenessSnapshot(
                    counts = CorpusCompletenessCounts(
                        memoriesReady = 3,
                        memoriesPendingAssembly = 0,
                        meaningSummaryIndexed = 2,
                        meaningEvidenceIndexed = 1,
                        meaningIndexPending = 0,
                    ),
                    blocked = null,
                ),
            ),
        )
        assertTrue(MeaningSearchCopy.SCOPE_BODY.contains("on-device meaning model"))
        assertTrue(ready.contains("Indexed on this phone"))
        assertTrue(ready.contains("Universal Sentence Encoder"))
        assertFalse(ready.contains("not a measured AVAILABLE"))
        assertFalse(ready.contains("not a claim"))
    }

    @Test
    fun why_cites_query_and_summary() {
        val why = MeaningSearchCopy.whyThisResult(
            hit = sampleHit(com.memora.app.domain.asset.AssetType.NOTE),
            query = "hotel near coffee",
        )
        assertTrue(why.contains("hotel near coffee"))
        assertTrue(why.contains("Hotel confirmation near the cafe"))
        assertTrue(why.contains("Why this result?"))
    }

    @Test
    fun why_and_open_hint_for_ranked_pdf_page() {
        val hit = sampleHit(com.memora.app.domain.asset.AssetType.PDF, citedPage = 1, rankedPage = 3)
        val why = MeaningSearchCopy.whyThisResult(hit, "mira")
        assertTrue(why.contains("Page 3"))
        val hint = MeaningSearchCopy.openOriginalPdfHint(
            citedPdfPageNumber = 1,
            rankedPdfPageNumber = 3,
        )
        assertTrue(hint.contains("page 3"))
        assertFalse(hint.contains("not measured AVAILABLE"))
        assertTrue(MeaningSearchCopy.rankedPdfPageLabel(3).contains("3"))
    }

    @Test
    fun open_hint_without_page_stays_short_and_plain() {
        val hint = MeaningSearchCopy.openOriginalPdfHint(null)
        assertTrue(hint.contains("Opens the original file"))
        assertFalse(hint.contains("cue-best"))
        assertFalse(hint.contains("fallback"))
        assertFalse(hint.contains("not a live re-read"))
        assertFalse(hint.contains("meaning index"))
    }

    @Test
    fun why_discloses_evidence_token_boost() {
        val why = MeaningSearchCopy.whyThisResult(
            hit = sampleHit(com.memora.app.domain.asset.AssetType.PDF, citedPage = 1, rankedPage = 3, boosted = true),
            query = "mira",
        )
        assertTrue(why.contains("Page 3"))
        assertTrue(why.contains("Your cue words appear"))
    }

    private fun sampleHit(
        type: com.memora.app.domain.asset.AssetType,
        citedPage: Int? = null,
        rankedPage: Int? = null,
        boosted: Boolean = false,
    ) = com.memora.app.application.intelligence.MeaningSearchHit(
        revisionId = com.memora.app.domain.memory.MemoryRevisionId("rev"),
        memoryId = com.memora.app.domain.memory.MemoryId("mem"),
        sourceId = com.memora.app.domain.asset.SourceId("src"),
        sourceAssetKey = com.memora.app.domain.asset.SourceAssetKey("asset"),
        assetType = type,
        label = "Label",
        summaryText = "Hotel confirmation near the cafe district",
        score = 0.5f,
        model = ModelVersionIdentity("m", "1"),
        citedPdfPageNumber = citedPage,
        rankedPdfPageNumber = rankedPage,
        evidenceTokenBoosted = boosted,
    )
}
