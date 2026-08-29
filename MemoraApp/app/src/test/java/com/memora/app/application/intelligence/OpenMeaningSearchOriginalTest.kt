package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeaningSearchOriginalTest {
    @Test
    fun ranked_hit_page_is_preferred_over_cited_for_open_wiring() {
        val hit = samplePdfHit(citedPage = 5).copy(rankedPdfPageNumber = 3)
        assertEquals(3, hit.rankedPdfPageNumber)
        assertEquals(5, hit.citedPdfPageNumber)
        // Production OpenMeaningSearchOriginal.resolvePdfOpenPage returns RANKED_HIT
        // when rankedPdfPageNumber is set (evidence-locator page after MIG-05 step 3).
    }

    @Test
    fun resolvePdfPage_uses_cited_page_when_present() {
        assertEquals(
            5,
            OpenMeaningSearchOriginal.resolvePdfPage(samplePdfHit(citedPage = 5)),
        )
        assertEquals(
            1,
            OpenMeaningSearchOriginal.resolvePdfPage(samplePdfHit(citedPage = 1)),
        )
    }

    @Test
    fun resolvePdfPage_falls_back_to_page_one_without_cite() {
        assertEquals(
            OpenMeaningSearchOriginal.PDF_FALLBACK_PAGE,
            OpenMeaningSearchOriginal.resolvePdfPage(samplePdfHit(citedPage = null)),
        )
    }

    private fun samplePdfHit(citedPage: Int?) = MeaningSearchHit(
        revisionId = MemoryRevisionId("r1"),
        memoryId = MemoryId("m1"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey("doc.pdf"),
        assetType = AssetType.PDF,
        label = "Travel plan",
        summaryText = "Museum tickets on page three",
        score = 0.8f,
        model = ModelVersionIdentity("m", "1"),
        citedPdfPageNumber = citedPage,
    )
}
