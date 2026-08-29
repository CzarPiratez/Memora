package com.memora.app.application.memory

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId

/**
 * Read port for MIG-06 literal search over stored MemoryEvidence excerpts.
 *
 * Implementations query the Memory evidence substrate (not extraction tables).
 */
interface MemoryEvidenceExcerptSearch {
    /**
     * Evidence rows on current-fingerprint READY Memories with non-blank excerpts.
     *
     * When [assetType] is non-null, only that asset type is counted (MIG-07
     * per-asset Find cutover). Null keeps the MIG-06 “all types” default.
     */
    suspend fun countCurrentReadyEvidence(
        assemblySchemaVersion: String,
        assetType: AssetType? = null,
    ): Int

    /**
     * Evidence + distinct-document counts for readiness inventory.
     *
     * [assetType] null = all types; non-null scopes to that asset type.
     */
    suspend fun countCurrentReadyEvidenceCorpus(
        assemblySchemaVersion: String,
        assetType: AssetType? = null,
    ): MemoryEvidenceCorpusCounts

    /**
     * Literal LIKE matches against evidence excerpts for current-fingerprint READY
     * Memories. [escapedNeedle] must already be escaped for SQL LIKE ESCAPE '\'.
     *
     * When [assetType] is non-null, only that asset type is searched.
     */
    suspend fun searchByExcerpt(
        escapedNeedle: String,
        assemblySchemaVersion: String,
        limit: Int,
        assetType: AssetType? = null,
    ): List<MemoryEvidenceExcerptMatch>
}

/** READY Memory-evidence inventory for honest Find readiness. */
data class MemoryEvidenceCorpusCounts(
    val evidenceCount: Int,
    val documentCount: Int,
) {
    init {
        require(evidenceCount >= 0) { "Evidence count cannot be negative." }
        require(documentCount >= 0) { "Document count cannot be negative." }
        if (evidenceCount == 0) {
            require(documentCount == 0) {
                "Empty evidence corpus cannot report documents."
            }
        } else {
            require(documentCount > 0) {
                "Non-empty evidence corpus needs at least one document."
            }
        }
    }
}

/**
 * One stored evidence row that matched a literal query, with asset identity for
 * future MIG-07 open-original / Why wiring.
 */
data class MemoryEvidenceExcerptMatch(
    val memoryId: MemoryId,
    val revisionId: MemoryRevisionId,
    val evidenceId: MemoryEvidenceId,
    val kind: MemoryEvidenceKind,
    val locator: EvidenceLocator,
    val excerpt: String,
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
    val assetType: AssetType,
    val displayLabel: String,
) {
    init {
        require(excerpt.isNotBlank()) { "Evidence excerpt match needs a stored excerpt." }
        require(displayLabel.isNotBlank()) { "Evidence excerpt match needs a display label." }
    }
}
