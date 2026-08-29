package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryIntegrityState
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * MIG-05 cutover: mark gap READY revisions
 * [MemoryIntegrityState.STALE_REINDEX_REQUIRED] when they have ≥1
 * `pdf:page:N` MemoryEvidence but zero evidence embeddings for [model], and
 * restore READY once evidence embeddings exist.
 *
 * Trigger: first readiness / search path (and after meaning-index taps).
 * Selection is intentional and narrow — never mass-STALE.
 * MIG-05 step 4: does not use PdfPageEmbeddingStore (retired).
 */
@Singleton
class ApplyMig05EvidenceSearchCutover @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
) {
    private val mutex = Mutex()

    suspend fun ensureApplied(
        model: ModelVersionIdentity,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Mig05EvidenceSearchCutoverResult = mutex.withLock {
        val readyRevisionIds = memoryRepository.listCurrentReadyRevisionIds()
        val pdfPageEvidenceRevisionIds =
            memoryRepository.findPdfPageEvidenceIds(readyRevisionIds).keys
        val evidenceRevisionIds = evidenceEmbeddingStore.listForModel(model)
            .mapTo(linkedSetOf()) { it.revisionId }

        val gaps = Mig05EvidenceSearchCutoverSelection.selectGapRevisionIds(
            readyRevisionIds = readyRevisionIds,
            revisionIdsWithPdfPageEvidence = pdfPageEvidenceRevisionIds,
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
