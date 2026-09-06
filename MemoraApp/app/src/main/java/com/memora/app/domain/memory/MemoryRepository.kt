package com.memora.app.domain.memory

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity

/**
 * Revision-safe persistence for Asset Memories.
 *
 * Implementations insert immutable revisions keyed by Asset identity, fingerprint,
 * and assembly schema. Existing history is never updated in place.
 */
interface MemoryRepository {
    suspend fun find(
        assetIdentity: AssetIdentity,
        assetFingerprint: AssetFingerprint,
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Memory?

    suspend fun insert(memory: Memory): MemoryInsertResult

    /** Number of READY revisions matching their Asset's current fingerprint. */
    suspend fun countCurrentReady(): Int

    /**
     * READY + [MemoryIntegrityState.STALE_REINDEX_REQUIRED] revisions matching
     * the Asset's current fingerprint (meaning-index drain after MIG-05 cutover).
     */
    suspend fun countMeaningIndexCandidates(): Int

    /** Lightweight summary rows for embedding index drains (current fingerprint only). */
    suspend fun listCurrentReadySummaries(limit: Int): List<MemoryEmbeddingSummary>

    /**
     * Meaning-index work still owed for [model]: no summary embedding yet,
     * [MemoryIntegrityState.STALE_REINDEX_REQUIRED], or READY revisions that
     * still have embeddable PDF / OCR / note evidence without an evidence
     * embedding for this model.
     *
     * Shares its selection rule with [listMeaningIndexSummaries], so a non-zero
     * count always means a non-empty batch is available.
     */
    suspend fun countMeaningIndexPending(model: ModelVersionIdentity): Int

    /**
     * Next meaning-index batch for [model]: current-fingerprint READY revisions
     * without a summary embedding, [MemoryIntegrityState.STALE_REINDEX_REQUIRED]
     * rows, and READY revisions that still owe embeddable evidence embeddings.
     * Already-complete revisions are excluded so repeated drains advance.
     */
    suspend fun listMeaningIndexSummaries(
        model: ModelVersionIdentity,
        limit: Int,
    ): List<MemoryEmbeddingSummary>

    /** Current-fingerprint READY revision ids (cutover gap selection). */
    suspend fun listCurrentReadyRevisionIds(): Set<MemoryRevisionId>

    /**
     * Current-fingerprint [MemoryIntegrityState.STALE_REINDEX_REQUIRED]
     * revision ids (cutover READY restore).
     */
    suspend fun listCurrentStaleReindexRevisionIds(): Set<MemoryRevisionId>

    /**
     * Transitions integrity state for the given revisions when they currently
     * match [from]. Returns how many rows were updated.
     */
    suspend fun markIntegrityState(
        revisionIds: Collection<MemoryRevisionId>,
        from: MemoryIntegrityState,
        to: MemoryIntegrityState,
        nowEpochMs: Long,
    ): Int

    /**
     * Current-fingerprint READY Memory rows for meaning-hit display, keyed by revision.
     * Missing / [MemoryIntegrityState.STALE_REINDEX_REQUIRED] revisions are omitted.
     */
    suspend fun findCurrentReadyMeaningLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, MemoryMeaningLookup>

    /**
     * Current-fingerprint READY + [MemoryIntegrityState.STALE_REINDEX_REQUIRED]
     * Memory rows for meaning-index evidence drains (and meaning hit display while
     * evidence embeddings catch up). Missing revisions are omitted.
     */
    suspend fun findMeaningIndexLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, MemoryMeaningLookup>

    /**
     * Maps revision → (1-based pageNumber → [MemoryEvidenceId]) for evidence
     * whose locator is `pdf:page:N`. Used by MIG-05 PDF page evidence embedding
     * so the evidence store is keyed by real evidence ids (`e{n}`), never by
     * the locator string. Also used by cutover gap selection after
     * PdfPageEmbedding* retirement (step 4). Missing revisions/pages are
     * omitted; ids are never invented.
     */
    suspend fun findPdfPageEvidenceIds(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, Map<Int, MemoryEvidenceId>>

    /**
     * Evidence rows for meaning-search hit text / page resolve, keyed by
     * revision then evidence id. Missing revisions/ids are omitted.
     */
    suspend fun findEvidenceSearchRows(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>>

    /**
     * OCR_TEXT evidence rows for meaning-index embedding drains (MIG-05 claim B
     * slice 1). Missing revisions are omitted; ids are never invented.
     */
    suspend fun findOcrTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>>

    /**
     * NOTE_TEXT evidence rows for meaning-index embedding drains (MIG-05 claim B
     * slice 2). Missing revisions are omitted; ids are never invented.
     */
    suspend fun findNoteTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>>

    /**
     * Signature anchors for structured recall (MIG-07B). Missing revisions omitted.
     */
    suspend fun findSignatureAnchors(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, List<MemoryAnchor>>
}

data class MemoryEmbeddingSummary(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val summaryText: String,
)

/** Source-joined Memory fields needed to render a meaning-search hit. */
data class MemoryMeaningLookup(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
    val assetType: AssetType,
    val displayLabel: String,
    val summaryText: String,
    /**
     * 1-based PDF page cited by the Memory summary evidence, when the locator
     * is `pdf:page:N`. Null when unknown or not a PDF page cite.
     */
    val citedPdfPageNumber: Int? = null,
) {
    init {
        require(displayLabel.isNotBlank()) { "Meaning lookup needs a display label." }
        require(summaryText.isNotBlank()) { "Meaning lookup needs summary text." }
        require(citedPdfPageNumber == null || citedPdfPageNumber > 0) {
            "Cited PDF page must be positive when present."
        }
    }
}

/** Evidence excerpt + locator for meaning-search scoring and PDF page open. */
data class MemoryEvidenceSearchRow(
    val revisionId: MemoryRevisionId,
    val evidenceId: MemoryEvidenceId,
    val locator: String,
    val excerpt: String,
) {
    init {
        require(locator.isNotBlank()) { "Evidence search row needs a locator." }
        require(excerpt.isNotBlank()) { "Evidence search row needs an excerpt." }
    }
}

sealed interface MemoryInsertResult {
    data object Inserted : MemoryInsertResult

    /** The exact immutable revision was already present; the write is idempotent. */
    data object AlreadyExists : MemoryInsertResult

    /** The key existed with different content, so history was left untouched. */
    data object RevisionConflict : MemoryInsertResult

    data object FailedSafely : MemoryInsertResult
}
