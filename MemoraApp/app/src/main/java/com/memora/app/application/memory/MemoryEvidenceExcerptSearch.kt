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
    /** Evidence rows on current-fingerprint READY Memories with non-blank excerpts. */
    suspend fun countCurrentReadyEvidence(assemblySchemaVersion: String): Int

    /**
     * Literal LIKE matches against evidence excerpts for current-fingerprint READY
     * Memories. [escapedNeedle] must already be escaped for SQL LIKE ESCAPE '\'.
     */
    suspend fun searchByExcerpt(
        escapedNeedle: String,
        assemblySchemaVersion: String,
        limit: Int,
    ): List<MemoryEvidenceExcerptMatch>
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
