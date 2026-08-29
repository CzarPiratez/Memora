package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.PdfPageEmbeddingStore
import com.memora.app.domain.memory.MemoryIntegrityState
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * MIG-05 step 3 cutover: mark gap READY revisions
 * [MemoryIntegrityState.STALE_REINDEX_REQUIRED] when they have PDF page
 * embeddings for [model] but zero evidence embeddings, and restore READY once
 * evidence embeddings exist.
 *
 * Trigger: first readiness / search path (and after meaning-index taps).
 * Selection is intentional and narrow — never mass-STALE.
 */
@Singleton
class ApplyMig05EvidenceSearchCutover @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val pdfPageEmbeddingStore: PdfPageEmbeddingStore,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
) {
    private val mutex = Mutex()

    suspend fun ensureApplied(
        model: ModelVersionIdentity,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Mig05EvidenceSearchCutoverResult = mutex.withLock {
        val pdfRevisionIds = pdfPageEmbeddingStore.listForModel(model)
            .mapTo(linkedSetOf()) { it.revisionId }
        val evidenceRevisionIds = evidenceEmbeddingStore.listForModel(model)
            .mapTo(linkedSetOf()) { it.revisionId }

        val gaps = Mig05EvidenceSearchCutoverSelection.selectGapRevisionIds(
            readyRevisionIds = memoryRepository.listCurrentReadyRevisionIds(),
            revisionIdsWithPdfPageEmbeddings = pdfRevisionIds,
            revisionIdsWithEvidenceEmbeddings = evidenceRevisionIds,
        )
        val markedStale = if (gaps.isEmpty()) {
            0
        } else {
            memoryRepository.markIntegrityState(
                revisionIds = gaps,
                from = MemoryIntegrityState.READY,
                to = MemoryIntegrityState.STALE_REINDEX_REQUIRED,
                nowEpochMs = nowEpochMs,
            )
        }

        val restore = Mig05EvidenceSearchCutoverSelection.selectRestoreRevisionIds(
            staleReindexRevisionIds = memoryRepository.listCurrentStaleReindexRevisionIds(),
            revisionIdsWithEvidenceEmbeddings = evidenceRevisionIds,
        )
        val restoredReady = if (restore.isEmpty()) {
            0
        } else {
            memoryRepository.markIntegrityState(
                revisionIds = restore,
                from = MemoryIntegrityState.STALE_REINDEX_REQUIRED,
                to = MemoryIntegrityState.READY,
                nowEpochMs = nowEpochMs,
            )
        }

        Mig05EvidenceSearchCutoverResult(
            gapRevisionIds = gaps,
            markedStale = markedStale,
            restoredReady = restoredReady,
        )
    }
}

data class Mig05EvidenceSearchCutoverResult(
    val gapRevisionIds: Set<MemoryRevisionId>,
    val markedStale: Int,
    val restoredReady: Int,
)
