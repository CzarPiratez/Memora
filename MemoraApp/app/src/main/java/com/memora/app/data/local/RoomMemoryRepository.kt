package com.memora.app.data.local

import com.memora.app.application.memory.AssembleAssetMemoryFromExtractionFacts
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryEmbeddingSummary
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryInsertResult
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId

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

    override suspend fun findCurrentReadyMeaningLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ): Map<MemoryRevisionId, MemoryMeaningLookup> {
        if (revisionIds.isEmpty()) return emptyMap()
        val ids = revisionIds.map { it.value }.distinct()
        return database().memoryDao().findCurrentReadyMeaningLookups(
            assemblySchemaVersion = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value,
            revisionIds = ids,
        ).associate { row ->
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
            )
        }
    }
}
