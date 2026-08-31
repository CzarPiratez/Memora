package com.memora.app.application.intelligence

import com.memora.app.domain.memory.MemoryRevisionId

/**
 * Pure selection for MIG-05 evidence-search cutover STALE marking.
 *
 * After MIG-05 step 4 (PdfPageEmbedding* retired): targets only current READY
 * revisions that already have a **summary** embedding for the active model, have
 * ≥1 MemoryEvidence with a `pdf:page:N` locator, but **zero** evidence embeddings
 * for that same revision + model.
 *
 * Fresh Asset Memories (page evidence present, summary not yet indexed) are
 * **not** gaps — they stay READY so keyword Find and the next Build meaning
 * index tap can complete evidence indexing without a STALE deadlock.
 * Never mass-STALEs unrelated READY revisions.
 */
object Mig05EvidenceSearchCutoverSelection {
    fun selectGapRevisionIds(
        readyRevisionIds: Set<MemoryRevisionId>,
        revisionIdsWithPdfPageEvidence: Set<MemoryRevisionId>,
        revisionIdsWithSummaryEmbeddings: Set<MemoryRevisionId>,
        revisionIdsWithEvidenceEmbeddings: Set<MemoryRevisionId>,
    ): Set<MemoryRevisionId> =
        readyRevisionIds
            .intersect(revisionIdsWithPdfPageEvidence)
            .intersect(revisionIdsWithSummaryEmbeddings)
            .subtract(revisionIdsWithEvidenceEmbeddings)

    /**
     * STALE revisions that now have at least one evidence embedding for the
     * active model and may return to READY after evidence-index fill.
     */
    fun selectRestoreRevisionIds(
        staleReindexRevisionIds: Set<MemoryRevisionId>,
        revisionIdsWithEvidenceEmbeddings: Set<MemoryRevisionId>,
    ): Set<MemoryRevisionId> =
        staleReindexRevisionIds.intersect(revisionIdsWithEvidenceEmbeddings)
}
