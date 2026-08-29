package com.memora.app.application.documents

import com.memora.app.application.memory.MemoryEvidenceRetrievalPath
import com.memora.app.application.memory.MemoryEvidenceSearchHit
import com.memora.app.application.memory.MemoryEvidenceSearchOutcome
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MIG-07 PDF adapter: MemoryEvidence hits → PdfKeywordSearchHit. */
class PdfMemoryEvidenceKeywordAdapterTest {
    @Test
    fun maps_pdf_page_hit_with_open_page() {
        val hit = sampleHit(
            locator = PdfPageEvidenceLocator.formatLocator(4),
            openPageNumber = 4,
            excerpt = "…invoice clause…",
        )
        val mapped = PdfMemoryEvidenceKeywordAdapter.toPdfHitOrNull(hit)
        assertEquals("Contract.pdf", mapped?.label)
        assertEquals(4, mapped?.pageNumber)
        assertEquals("…invoice clause…", mapped?.excerpt)
        assertEquals("saf-document-tree", mapped?.sourceId)
        assertEquals("pdf-1", mapped?.sourceAssetKey)
    }

    @Test
    fun drops_hit_without_resolvable_page() {
        val hit = sampleHit(
            locator = "pdf:title",
            openPageNumber = null,
            excerpt = "Title mentions invoice",
        )
        assertNull(PdfMemoryEvidenceKeywordAdapter.toPdfHitOrNull(hit))
    }

    @Test
    fun maps_matches_outcome_and_drops_non_page_hits() {
        val outcome = MemoryEvidenceSearchOutcome.Matches(
            query = "invoice",
            hits = listOf(
                sampleHit(
                    locator = "pdf:title",
                    openPageNumber = null,
                    excerpt = "invoice title",
                ),
                sampleHit(
                    locator = PdfPageEvidenceLocator.formatLocator(2),
                    openPageNumber = 2,
                    excerpt = "page invoice",
                ),
            ),
            limitReached = false,
        )
        val pdf = PdfMemoryEvidenceKeywordAdapter.toPdfOutcome(outcome)
            as PdfKeywordSearchOutcome.Matches
        assertEquals(1, pdf.hits.size)
        assertEquals(2, pdf.hits.single().pageNumber)
        assertTrue(pdf.hits.single().excerpt.contains("invoice"))
    }

    private fun sampleHit(
        locator: String,
        openPageNumber: Int?,
        excerpt: String,
    ) = MemoryEvidenceSearchHit(
        memoryId = MemoryId("mem-doc"),
        revisionId = MemoryRevisionId("rev-doc"),
        evidenceId = MemoryEvidenceId("e-doc"),
        kind = MemoryEvidenceKind.DOCUMENT_TEXT,
        locator = EvidenceLocator(locator),
        excerpt = excerpt,
        sourceId = SourceId("saf-document-tree"),
        sourceAssetKey = SourceAssetKey("pdf-1"),
        assetType = AssetType.PDF,
        label = "Contract.pdf",
        openPageNumber = openPageNumber,
        retrievalPath = MemoryEvidenceRetrievalPath.KEYWORD,
    )
}
