package com.memora.app.data.local

import com.memora.app.application.memory.MemoryEvidenceCorpusCounts
import com.memora.app.application.memory.MemoryEvidenceExcerptMatch
import com.memora.app.application.memory.MemoryEvidenceExcerptSearch
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId

/**
 * Room adapter for [MemoryEvidenceExcerptSearch].
 *
 * Queries [MemoryDao] only — no extraction-table Find path.
 */
class RoomMemoryEvidenceExcerptSearch(
    private val memoryDao: () -> MemoryDao,
) : MemoryEvidenceExcerptSearch {
    override suspend fun countCurrentReadyEvidence(
        assemblySchemaVersion: String,
        assetType: AssetType?,
    ): Int =
        memoryDao().countCurrentReadyEvidence(
            assemblySchemaVersion = assemblySchemaVersion,
            assetType = assetType?.name,
        )

    override suspend fun countCurrentReadyEvidenceCorpus(
        assemblySchemaVersion: String,
        assetType: AssetType?,
    ): MemoryEvidenceCorpusCounts {
        val row = memoryDao().countCurrentReadyEvidenceCorpus(
            assemblySchemaVersion = assemblySchemaVersion,
            assetType = assetType?.name,
        )
        return MemoryEvidenceCorpusCounts(
            evidenceCount = row.evidenceCount,
            documentCount = row.documentCount,
        )
    }

    override suspend fun searchByExcerpt(
        escapedNeedle: String,
        assemblySchemaVersion: String,
        limit: Int,
        assetType: AssetType?,
    ): List<MemoryEvidenceExcerptMatch> {
        require(limit > 0)
        return memoryDao().searchCurrentReadyEvidenceByExcerpt(
            escapedNeedle = escapedNeedle,
            assemblySchemaVersion = assemblySchemaVersion,
            limit = limit,
            assetType = assetType?.name,
        ).mapNotNull { row -> row.toMatchOrNull() }
    }

    private fun MemoryEvidenceLiteralSearchRowEntity.toMatchOrNull(): MemoryEvidenceExcerptMatch? {
        if (locator.isBlank() || excerpt.isBlank()) return null
        val kind = runCatching { MemoryEvidenceKind.valueOf(evidenceKind) }.getOrNull()
            ?: return null
        val parsedAssetType = runCatching { AssetType.valueOf(assetType) }.getOrNull()
            ?: return null
        val label = displayName?.takeIf { it.isNotBlank() }
            ?: sourceAssetKey.takeIf { it.isNotBlank() }
            ?: return null
        return MemoryEvidenceExcerptMatch(
            memoryId = MemoryId(memoryId),
            revisionId = MemoryRevisionId(revisionId),
            evidenceId = MemoryEvidenceId(evidenceId),
            kind = kind,
            locator = EvidenceLocator(locator),
            excerpt = excerpt,
            sourceId = SourceId(sourceId),
            sourceAssetKey = SourceAssetKey(sourceAssetKey),
            assetType = parsedAssetType,
            displayLabel = label,
        )
    }
}
