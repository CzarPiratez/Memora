package com.memora.app.domain.memory

import com.memora.app.domain.asset.Asset

/** One bounded deterministic fact already persisted for an Asset version. */
data class AssetMemoryFact(
    val kind: MemoryEvidenceKind,
    val locator: String,
    val excerpt: String,
    val extractionSchemaVersion: String,
) {
    init {
        require(
            kind == MemoryEvidenceKind.OCR_TEXT ||
                kind == MemoryEvidenceKind.DOCUMENT_TEXT ||
                kind == MemoryEvidenceKind.NOTE_TEXT ||
                kind == MemoryEvidenceKind.SOURCE_METADATA,
        ) { "Pre-AI Asset Memories may use deterministic evidence only." }
        require(locator.isNotBlank())
        require(excerpt.isNotBlank())
        require(extractionSchemaVersion.isNotBlank())
    }
}

/** Read-only view over current-fingerprint extraction records. */
interface AssetMemoryFactSource {
    suspend fun loadCurrentFacts(asset: Asset): List<AssetMemoryFact>

    /** Next current Asset that has usable facts but no revision for [assemblySchemaVersion]. */
    suspend fun findNextPendingAsset(
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Asset?
}
