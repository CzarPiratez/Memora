package com.memora.app.data.local

import com.memora.app.application.memory.AssembleAssetMemoryFromExtractionFacts
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryEmbeddingSummary
import com.memora.app.domain.memory.MemoryAnchor
import com.memora.app.domain.memory.MemoryAnchorId
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryText
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceSearchRow
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryInsertResult
import com.memora.app.domain.memory.MemoryIntegrityState
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator

class RoomMemoryRepository(
    private val database: () -> MemoraDatabase,
) : MemoryRepository {
    override suspend fun find(
        assetIdentity: AssetIdentity,
        assetFingerprint: AssetFingerprint,
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Memory? = database().memoryDao().findRows(
        sourceId = assetIdentity.sourceId.value,
        sourceAssetKey = assetIdentity.sourceAssetKey.value,
        fingerprint = assetFingerprint.value,
        assemblySchemaVersion = assemblySchemaVersion.value,
    )?.let(MemoryRoomMapper::toDomain)

    override suspend fun insert(memory: Memory): MemoryInsertResult {
        val dao = database().memoryDao()
        return try {
            val existing = find(
                memory.assetIdentity,
                memory.assetFingerprint,
                memory.assemblySchemaVersion,
            )
            if (existing != null) {
                return if (existing == memory) {
                    MemoryInsertResult.AlreadyExists
                } else {
                    MemoryInsertResult.RevisionConflict
                }
            }

            dao.insertAtomic(MemoryRoomMapper.toRows(memory))
            MemoryInsertResult.Inserted
        } catch (_: Exception) {
            val raced = runCatching {
                find(memory.assetIdentity, memory.assetFingerprint, memory.assemblySchemaVersion)
            }.getOrNull()
            if (raced == memory) MemoryInsertResult.AlreadyExists else MemoryInsertResult.FailedSafely
        }
    }

    override suspend fun countCurrentReady(): Int =
        database().memoryDao().countCurrentReady(
            assemblySchemaVersion = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value,
        )

    override suspend fun countMeaningIndexCandidates(): Int =
        database().memoryDao().countMeaningIndexCandidates(
            assemblySchemaVersion = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value,
        )

    override suspend fun listCurrentReadySummaries(limit: Int): List<MemoryEmbeddingSummary> {
        require(limit > 0)
        return database().memoryDao().listCurrentReadySummaries(
            assemblySchemaVersion = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value,
            limit = limit,
        ).map { row ->
            MemoryEmbeddingSummary(
                revisionId = MemoryRevisionId(row.revisionId),
                memoryId = MemoryId(row.memoryId),
                summaryText = row.summaryText,
            )
        }
    }

    override suspend fun countMeaningIndexPending(model: ModelVersionIdentity): Int =
        database().memoryDao().countMeaningIndexPending(
            assemblySchemaVersion = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value,
            modelId = model.modelId,
            modelVersion = model.version,
        )

    override suspend fun listMeaningIndexSummaries(
        model: ModelVersionIdentity,
        limit: Int,
    ): List<MemoryEmbeddingSummary> {
        require(limit > 0)
        return database().memoryDao().listMeaningIndexSummaries(
            assemblySchemaVersion = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value,
            modelId = model.modelId,
            modelVersion = model.version,
            limit = limit,
        ).map { row ->
            MemoryEmbeddingSummary(
                revisionId = MemoryRevisionId(row.revisionId),
                memoryId = MemoryId(row.memoryId),
                summaryText = row.summaryText,
            )
        }
    }

    override suspend fun listCurrentReadyRevisionIds(): Set<MemoryRevisionId> =
        database().memoryDao().listCurrentRevisionIdsByIntegrity(
            assemblySchemaVersion = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value,
            integrityState = MemoryIntegrityState.READY.name,
        ).mapTo(linkedSetOf()) { MemoryRevisionId(it) }

    override suspend fun listCurrentStaleReindexRevisionIds(): Set<MemoryRevisionId> =
        database().memoryDao().listCurrentRevisionIdsByIntegrity(
            assemblySchemaVersion = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value,
            integrityState = MemoryIntegrityState.STALE_REINDEX_REQUIRED.name,
        ).mapTo(linkedSetOf()) { MemoryRevisionId(it) }

    override suspend fun markIntegrityState(
        revisionIds: Collection<MemoryRevisionId>,
        from: MemoryIntegrityState,
        to: MemoryIntegrityState,
        nowEpochMs: Long,
    ): Int {
        if (revisionIds.isEmpty()) return 0
        require(nowEpochMs >= 0)
        return database().memoryDao().markIntegrityState(
            revisionIds = revisionIds.map { it.value }.distinct(),
            fromState = from.name,
            toState = to.name,
            nowEpochMs = nowEpochMs,
        )
    }

    override suspend fun findCurrentReadyMeaningLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, MemoryMeaningLookup> =
        meaningLookups(
            revisionIds = revisionIds,
            query = { dao, schema, ids ->
                dao.findCurrentReadyMeaningLookups(schema, ids)
            },
        )

    override suspend fun findMeaningIndexLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, MemoryMeaningLookup> =
        meaningLookups(
            revisionIds = revisionIds,
            query = { dao, schema, ids ->
                dao.findMeaningIndexLookups(schema, ids)
            },
        )

    private suspend fun meaningLookups(
        revisionIds: Collection<MemoryRevisionId>,
        query: suspend (
            MemoryDao,
            String,
            List<String>,
        ) -> List<MemoryMeaningLookupRow>,
    ): Map<MemoryRevisionId, MemoryMeaningLookup> {
        if (revisionIds.isEmpty()) return emptyMap()
        val ids = revisionIds.map { it.value }.distinct()
        val dao = database().memoryDao()
        val citedPagesByRevision = dao.findSummaryEvidenceLocators(ids)
            .groupBy { it.revisionId }
            .mapValues { (_, rows) ->
                PdfPageEvidenceLocator.firstPageNumber(rows.map { it.locator })
            }
        val schema = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value
        return query(dao, schema, ids).associate { row ->
            val revisionId = MemoryRevisionId(row.revisionId)
            revisionId to MemoryMeaningLookup(
                revisionId = revisionId,
                memoryId = MemoryId(row.memoryId),
                sourceId = SourceId(row.sourceId),
                sourceAssetKey = SourceAssetKey(row.sourceAssetKey),
                assetType = AssetType.valueOf(row.assetType),
                displayLabel = row.displayName?.takeIf { it.isNotBlank() }
                    ?: row.sourceAssetKey,
                summaryText = row.summaryText,
                citedPdfPageNumber = citedPagesByRevision[row.revisionId],
            )
        }
    }

    override suspend fun findPdfPageEvidenceIds(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, Map<Int, MemoryEvidenceId>> {
        if (revisionIds.isEmpty()) return emptyMap()
        val ids = revisionIds.map { it.value }.distinct()
        val result = linkedMapOf<MemoryRevisionId, MutableMap<Int, MemoryEvidenceId>>()
        for (row in database().memoryDao().findEvidenceLocators(ids)) {
            val pageNumber = PdfPageEvidenceLocator.parsePageNumber(row.locator) ?: continue
            // Never treat a locator-shaped string as an evidence id.
            if (PdfPageEvidenceLocator.parsePageNumber(row.evidenceId) != null) continue
            val revisionId = MemoryRevisionId(row.revisionId)
            val pages = result.getOrPut(revisionId) { linkedMapOf() }
            // First matching evidence wins (stable ORDER BY evidence_id).
            pages.putIfAbsent(pageNumber, MemoryEvidenceId(row.evidenceId))
        }
        return result
    }

    override suspend fun findEvidenceSearchRows(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>> {
        if (revisionIds.isEmpty()) return emptyMap()
        val ids = revisionIds.map { it.value }.distinct()
        val result =
            linkedMapOf<MemoryRevisionId, MutableMap<MemoryEvidenceId, MemoryEvidenceSearchRow>>()
        for (row in database().memoryDao().findEvidenceSearchRows(ids)) {
            if (row.locator.isBlank() || row.excerpt.isBlank()) continue
            val revisionId = MemoryRevisionId(row.revisionId)
            val evidenceId = MemoryEvidenceId(row.evidenceId)
            val byEvidence = result.getOrPut(revisionId) { linkedMapOf() }
            byEvidence.putIfAbsent(
                evidenceId,
                MemoryEvidenceSearchRow(
                    revisionId = revisionId,
                    evidenceId = evidenceId,
                    locator = row.locator,
                    excerpt = row.excerpt,
                ),
            )
        }
        return result
    }

    override suspend fun findOcrTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>> {
        if (revisionIds.isEmpty()) return emptyMap()
        val ids = revisionIds.map { it.value }.distinct()
        val result = linkedMapOf<MemoryRevisionId, MutableList<MemoryEvidenceSearchRow>>()
        for (row in database().memoryDao().findOcrTextEvidenceForEmbedding(ids)) {
            if (row.locator.isBlank() || row.excerpt.isBlank()) continue
            val revisionId = MemoryRevisionId(row.revisionId)
            val rows = result.getOrPut(revisionId) { mutableListOf() }
            rows += MemoryEvidenceSearchRow(
                revisionId = revisionId,
                evidenceId = MemoryEvidenceId(row.evidenceId),
                locator = row.locator,
                excerpt = row.excerpt,
            )
        }
        return result
    }

    override suspend fun findNoteTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>> {
        if (revisionIds.isEmpty()) return emptyMap()
        val ids = revisionIds.map { it.value }.distinct()
        val result = linkedMapOf<MemoryRevisionId, MutableList<MemoryEvidenceSearchRow>>()
        for (row in database().memoryDao().findNoteTextEvidenceForEmbedding(ids)) {
            if (row.locator.isBlank() || row.excerpt.isBlank()) continue
            val revisionId = MemoryRevisionId(row.revisionId)
            val rows = result.getOrPut(revisionId) { mutableListOf() }
            rows += MemoryEvidenceSearchRow(
                revisionId = revisionId,
                evidenceId = MemoryEvidenceId(row.evidenceId),
                locator = row.locator,
                excerpt = row.excerpt,
            )
        }
        return result
    }

    override suspend fun findSignatureAnchors(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, List<MemoryAnchor>> {
        if (revisionIds.isEmpty()) return emptyMap()
        val ids = revisionIds.map { it.value }.distinct()
        val dao = database().memoryDao()
        val evidenceByRevisionAndAnchor = dao.findAnchorEvidenceForRevisions(ids)
            .groupBy { it.revisionId to it.anchorId }
            .mapValues { (_, rows) ->
                rows.mapTo(linkedSetOf()) { MemoryEvidenceId(it.evidenceId) }
            }
        val result = linkedMapOf<MemoryRevisionId, MutableList<MemoryAnchor>>()
        for (entity in dao.findAnchorsForRevisions(ids)) {
            val kind = runCatching { MemoryAnchorKind.valueOf(entity.anchorKind) }.getOrNull()
                ?: continue
            val anchorText = entity.anchorText.trim()
            if (anchorText.isBlank()) continue
            val evidenceIds = evidenceByRevisionAndAnchor[entity.revisionId to entity.anchorId]
                .orEmpty()
            if (evidenceIds.isEmpty()) continue
            val revisionId = MemoryRevisionId(entity.revisionId)
            val anchors = result.getOrPut(revisionId) { mutableListOf() }
            anchors += MemoryAnchor(
                id = MemoryAnchorId(entity.anchorId),
                kind = kind,
                text = MemoryText(anchorText),
                evidenceIds = evidenceIds,
            )
        }
        return result
    }
}
