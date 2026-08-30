package com.memora.app.application.memory

import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId

/**
 * Test-only delegate that throws for unimplemented [MemoryRepository] methods.
 */
internal class EmptyMemoryRepositoryDelegate : MemoryRepository {
    override suspend fun find(
        assetIdentity: com.memora.app.domain.asset.AssetIdentity,
        assetFingerprint: com.memora.app.domain.asset.AssetFingerprint,
        assemblySchemaVersion: com.memora.app.domain.memory.MemoryAssemblySchemaVersion,
    ) = error("not used")

    override suspend fun insert(memory: com.memora.app.domain.memory.Memory) =
        error("not used")

    override suspend fun countCurrentReady(): Int = error("not used")

    override suspend fun countMeaningIndexCandidates(): Int = error("not used")

    override suspend fun listCurrentReadySummaries(limit: Int) = error("not used")

    override suspend fun listMeaningIndexSummaries(limit: Int) = error("not used")

    override suspend fun listCurrentReadyRevisionIds(): Set<MemoryRevisionId> = error("not used")

    override suspend fun listCurrentStaleReindexRevisionIds(): Set<MemoryRevisionId> =
        error("not used")

    override suspend fun markIntegrityState(
        revisionIds: Collection<MemoryRevisionId>,
        from: com.memora.app.domain.memory.MemoryIntegrityState,
        to: com.memora.app.domain.memory.MemoryIntegrityState,
        nowEpochMs: Long,
    ): Int = error("not used")

    override suspend fun findCurrentReadyMeaningLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ) = error("not used")

    override suspend fun findPdfPageEvidenceIds(
        revisionIds: Collection<MemoryRevisionId>,
    ) = error("not used")

    override suspend fun findEvidenceSearchRows(
        revisionIds: Collection<MemoryRevisionId>,
    ) = error("not used")

    override suspend fun findOcrTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ) = error("not used")

    override suspend fun findNoteTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ) = error("not used")

    override suspend fun findSignatureAnchors(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, List<com.memora.app.domain.memory.MemoryAnchor>>()
}
