package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchReadiness
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningSearchCopyTest {
    @Test
    fun scope_and_readiness_stay_candidate_not_available() {
        val ready = MeaningSearchCopy.readinessBody(
            MeaningSearchReadiness.Ready(
                model = ModelVersionIdentity("m", "1"),
                indexedCount = 3,
                memoriesReadyCount = 3,
            ),
        )
        assertTrue(MeaningSearchCopy.SCOPE_BODY.contains("candidate", ignoreCase = true))
        assertTrue(ready.contains("AVAILABLE"))
        assertFalse(ready.contains("full measured meaning search is finished"))
        assertTrue(ready.contains("not a measured AVAILABLE"))
    }

    @Test
    fun why_cites_query_and_summary() {
        val why = MeaningSearchCopy.whyThisResult(
            hit = sampleHit(AssetType.NOTE),
            query = "hotel near coffee",
        )
        assertTrue(why.contains("hotel near coffee"))
        assertTrue(why.contains("Hotel confirmation near the cafe"))
        assertTrue(why.contains("Why this result?"))
    }

    @Test
    fun why_and_open_hint_cite_pdf_page_when_known() {
        val hit = sampleHit(AssetType.PDF, citedPage = 4)
        val why = MeaningSearchCopy.whyThisResult(hit, "museum tickets")
        assertTrue(why.contains("page 4"))
        val hint = MeaningSearchCopy.openOriginalPdfHint(4)
        assertTrue(hint.contains("page 4"))
        assertTrue(hint.contains("not a query-best"))
        assertFalse(hint.contains("always page 1"))
        assertTrue(MeaningSearchCopy.citedPdfPageLabel(4).contains("4"))
    }

    @Test
    fun open_hint_without_cite_admits_page_one_fallback() {
        val hint = MeaningSearchCopy.openOriginalPdfHint(null)
        assertTrue(hint.contains("page 1"))
        assertTrue(hint.contains("fallback"))
        assertTrue(hint.contains("not a live re-read"))
    }

    private fun sampleHit(
        type: AssetType,
        citedPage: Int? = null,
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId("r1"),
        memoryId = MemoryId("m1"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey("k"),
        assetType = type,
        label = "Trip plan",
        summaryText = "Hotel confirmation near the cafe",
        score = 0.8f,
        model = ModelVersionIdentity("m", "1"),
        citedPdfPageNumber = citedPage,
    )
}
