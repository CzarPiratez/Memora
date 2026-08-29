package com.memora.app.domain.memory

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId

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

    /** Lightweight summary rows for embedding index drains (current fingerprint only). */
    suspend fun listCurrentReadySummaries(limit: Int): List<MemoryEmbeddingSummary>

    /**
     * Current-fingerprint READY Memory rows for meaning-hit display, keyed by revision.
     * Missing / stale revisions are omitted.
     */
    suspend fun findCurrentReadyMeaningLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, MemoryMeaningLookup>

    /**
     * Maps revision → (1-based pageNumber → [MemoryEvidenceId]) for evidence
     * whose locator is `pdf:page:N`. Used by MIG-05 step 2 PDF page embedding
     * dual-write so the evidence store is keyed by real evidence ids (`e{n}`),
     * never by the locator string. Missing revisions/pages are omitted; ids
     * are never invented.
     */
    suspend fun findPdfPageEvidenceIds(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, Map<Int, MemoryEvidenceId>>
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

sealed interface MemoryInsertResult {
    data object Inserted : MemoryInsertResult

    /** The exact immutable revision was already present; the write is idempotent. */
    data object AlreadyExists : MemoryInsertResult

    /** The key existed with different content, so history was left untouched. */
    data object RevisionConflict : MemoryInsertResult

    data object FailedSafely : MemoryInsertResult
}
