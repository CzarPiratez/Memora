package com.memora.app.application.find

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.memory.CanonicalRecallTestFixtures
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindThumbnailPolicyTest {
    @Test
    fun missing_pdf_page_renders_first_page_and_is_not_cited() {
        assertEquals(1, FindThumbnailPolicy.pdfPageToRender(null))
        assertFalse(FindThumbnailPolicy.pageWasCited(null))
        assertEquals(7, FindThumbnailPolicy.pdfPageToRender(7))
        assertTrue(FindThumbnailPolicy.pageWasCited(7))
    }

    @Test
    fun failures_are_not_cacheable_notes_are() {
        assertFalse(
            FindThumbnailPolicy.cacheable(
                FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE),
            ),
        )
        assertTrue(
            FindThumbnailPolicy.cacheable(FindThumbnailResult.Glyph(FindThumbnailGlyph.NOTE)),
        )
        assertTrue(
            FindThumbnailPolicy.cacheable(
                FindThumbnailResult.Ready(1, 1, intArrayOf(0xFFFFFFFF.toInt()), 3, true),
            ),
        )
    }

    @Test
    fun request_from_recall_keeps_cited_page() {
        val request = FindThumbnailRequest.fromRecall(
            CanonicalRecallTestFixtures.keywordRecall(pageNumber = 4),
        )
        assertEquals(AssetType.PDF, request.assetType)
        assertEquals(4, request.pageNumber)
        assertTrue(request.cacheKey.contains("|4|"))
    }

    @Test
    fun meaning_prefers_ranked_page_over_cited_page() {
        val hit = MeaningSearchHit(
            revisionId = MemoryRevisionId("rev-1"),
            memoryId = MemoryId("mem-1"),
            sourceId = SourceId("source-1"),
            sourceAssetKey = SourceAssetKey("asset-1"),
            assetType = AssetType.PDF,
            label = "timetable.pdf",
            summaryText = "swimming times",
            score = 0.4f,
            model = ModelVersionIdentity("test-model", "1"),
            citedPdfPageNumber = 1,
            rankedPdfPageNumber = 9,
        )
        val request = FindThumbnailRequest.fromMeaning(hit)
        assertEquals(9, request.pageNumber)
    }
}
