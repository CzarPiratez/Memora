package com.memora.app.application.intelligence

import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mig05EvidenceSearchCutoverSelectionTest {
    private val readyA = MemoryRevisionId("rev-a")
    private val readyB = MemoryRevisionId("rev-b")
    private val readyC = MemoryRevisionId("rev-c")
    private val readyD = MemoryRevisionId("rev-d")

    @Test
    fun selects_only_ready_with_summary_pdf_page_evidence_and_zero_evidence_embeddings() {
        val gaps = Mig05EvidenceSearchCutoverSelection.selectGapRevisionIds(
            readyRevisionIds = setOf(readyA, readyB, readyC, readyD),
            revisionIdsWithPdfPageEvidence = setOf(readyA, readyB, readyC),
            revisionIdsWithSummaryEmbeddings = setOf(readyA, readyB, readyC, readyD),
            revisionIdsWithEvidenceEmbeddings = setOf(readyB),
        )
        assertEquals(setOf(readyA, readyC), gaps)
    }

    @Test
    fun does_not_select_fresh_ready_without_summary_embedding() {
        val gaps = Mig05EvidenceSearchCutoverSelection.selectGapRevisionIds(
            readyRevisionIds = setOf(readyA, readyB),
            revisionIdsWithPdfPageEvidence = setOf(readyA, readyB),
            revisionIdsWithSummaryEmbeddings = emptySet(),
            revisionIdsWithEvidenceEmbeddings = emptySet(),
        )
        assertTrue(gaps.isEmpty())
    }

    @Test
    fun does_not_select_ready_without_pdf_page_evidence() {
        val gaps = Mig05EvidenceSearchCutoverSelection.selectGapRevisionIds(
            readyRevisionIds = setOf(readyA, readyB),
            revisionIdsWithPdfPageEvidence = setOf(readyA),
            revisionIdsWithSummaryEmbeddings = setOf(readyA, readyB),
            revisionIdsWithEvidenceEmbeddings = emptySet(),
        )
        assertEquals(setOf(readyA), gaps)
        assertTrue(readyB !in gaps)
    }

    @Test
    fun does_not_select_when_evidence_embeddings_already_present() {
        val gaps = Mig05EvidenceSearchCutoverSelection.selectGapRevisionIds(
            readyRevisionIds = setOf(readyA),
            revisionIdsWithPdfPageEvidence = setOf(readyA),
            revisionIdsWithSummaryEmbeddings = setOf(readyA),
            revisionIdsWithEvidenceEmbeddings = setOf(readyA),
        )
        assertTrue(gaps.isEmpty())
    }

    @Test
    fun does_not_mass_stale_unrelated_ready_revisions() {
        val gaps = Mig05EvidenceSearchCutoverSelection.selectGapRevisionIds(
            readyRevisionIds = setOf(readyA, readyB, readyC, readyD),
            revisionIdsWithPdfPageEvidence = setOf(readyA),
            revisionIdsWithSummaryEmbeddings = setOf(readyA, readyB, readyC, readyD),
            revisionIdsWithEvidenceEmbeddings = emptySet(),
        )
        assertEquals(setOf(readyA), gaps)
    }

    @Test
    fun restore_selects_stale_that_now_have_evidence_embeddings() {
        val staleA = MemoryRevisionId("stale-a")
        val staleB = MemoryRevisionId("stale-b")
        val restore = Mig05EvidenceSearchCutoverSelection.selectRestoreRevisionIds(
            staleReindexRevisionIds = setOf(staleA, staleB),
            revisionIdsWithEvidenceEmbeddings = setOf(staleA),
        )
        assertEquals(setOf(staleA), restore)
    }
}
