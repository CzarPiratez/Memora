package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalRecallResultTest {
    @Test
    fun memory_evidence_hit_maps_keyword_fields() {
        val hit = MemoryEvidenceSearchHit(
            memoryId = MemoryId("mem"),
            revisionId = MemoryRevisionId("rev"),
            evidenceId = MemoryEvidenceId("ev"),
            kind = MemoryEvidenceKind.OCR_TEXT,
            locator = EvidenceLocator(PdfPageEvidenceLocator.formatLocator(2)),
            excerpt = "meet mira tomorrow",
            sourceId = SourceId("src"),
            sourceAssetKey = SourceAssetKey("asset"),
            assetType = AssetType.PDF,
            label = "doc.pdf",
            openPageNumber = 2,
        )

        val result = hit.toCanonicalRecallResult()

        assertEquals(MemoryId("mem"), result.memoryId)
        assertEquals(MemoryRevisionId("rev"), result.revisionId)
        assertEquals(MemoryEvidenceId("ev"), result.evidenceId)
        assertEquals(CanonicalRecallRetrievalPath.KEYWORD, result.retrievalPath)
        assertEquals(2, result.openPageNumber)
        assertNull(result.rankScore)
    }

    @Test
    fun meaning_hit_maps_ranked_page_and_boost() {
        val hit = MeaningSearchHit(
            revisionId = MemoryRevisionId("rev"),
            memoryId = MemoryId("mem"),
            sourceId = SourceId("src"),
            sourceAssetKey = SourceAssetKey("asset"),
            assetType = AssetType.PDF,
            label = "doc.pdf",
            summaryText = "Hotel near cafe",
            score = 0.42f,
            model = ModelVersionIdentity("m", "1"),
            citedPdfPageNumber = 1,
            rankedPdfPageNumber = 3,
            evidenceTokenBoosted = true,
        )

        val result = hit.toCanonicalRecallResult()

        assertEquals(CanonicalRecallRetrievalPath.MEANING, result.retrievalPath)
        assertEquals(3, result.openPageNumber)
        assertEquals(0.42f, result.rankScore)
        assertEquals(true, result.evidenceTokenBoosted)
        assertEquals(
            EvidenceLocator(PdfPageEvidenceLocator.formatLocator(3)),
            result.locator,
        )
    }
}
