package com.memora.app.application.intelligence

import com.memora.app.domain.memory.MemoryRevisionId

/**
 * Pure selection for MIG-05 step 3 cutover STALE marking.
 *
 * Targets only current READY revisions that already have PDF page embeddings
 * for the active model but have **zero** evidence embeddings for that same
 * revision + model. Never mass-STALEs unrelated READY revisions.
 */
object Mig05EvidenceSearchCutoverSelection {
    fun selectGapRevisionIds(
        readyRevisionIds: Set<MemoryRevisionId>,
        revisionIdsWithPdfPageEmbeddings: Set<MemoryRevisionId>,
        revisionIdsWithEvidenceEmbeddings: Set<MemoryRevisionId>,
    ): Set<MemoryRevisionId> =
        readyRevisionIds
            .intersect(revisionIdsWithPdfPageEmbeddings)
            .subtract(revisionIdsWithEvidenceEmbeddings)

    /**
     * STALE revisions that now have at least one evidence embedding for the
     * active model and may return to READY after dual-write fill.
     */
    fun selectRestoreRevisionIds(
        staleReindexRevisionIds: Set<MemoryRevisionId>,
        revisionIdsWithEvidenceEmbeddings: Set<MemoryRevisionId>,
    ): Set<MemoryRevisionId> =
        staleReindexRevisionIds.intersect(revisionIdsWithEvidenceEmbeddings)
}
