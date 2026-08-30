package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator

/**
 * Shared Canonical Recall hit shape (`CANONICAL_RECALL_RESULT_CONTRACT.md`).
 *
 * Product Find ViewModels map keyword and meaning candidates into this type
 * before Why presentation. One dialect across PDF, photo, screenshot, and note.
 */
enum class CanonicalRecallRetrievalPath {
    KEYWORD,
    MEANING,
}

data class CanonicalRecallResult(
    val memoryId: MemoryId,
    val revisionId: MemoryRevisionId,
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
    val assetType: AssetType,
    val label: String,
    val excerpt: String,
    val evidenceId: MemoryEvidenceId?,
    val locator: EvidenceLocator?,
    val retrievalPath: CanonicalRecallRetrievalPath,
    val rankScore: Float?,
    val openPageNumber: Int?,
    val evidenceTokenBoosted: Boolean = false,
) {
    init {
        require(label.isNotBlank())
        require(excerpt.isNotBlank())
        require(rankScore == null || rankScore.isFinite())
        require(openPageNumber == null || openPageNumber > 0)
    }
}

fun MemoryEvidenceSearchHit.toCanonicalRecallResult(): CanonicalRecallResult =
    CanonicalRecallResult(
        memoryId = memoryId,
        revisionId = revisionId,
        sourceId = sourceId,
        sourceAssetKey = sourceAssetKey,
        assetType = assetType,
        label = label,
        excerpt = excerpt,
        evidenceId = evidenceId,
        locator = locator,
        retrievalPath = when (retrievalPath) {
            MemoryEvidenceRetrievalPath.KEYWORD -> CanonicalRecallRetrievalPath.KEYWORD
        },
        rankScore = null,
        openPageNumber = openPageNumber,
        evidenceTokenBoosted = false,
    )

fun MeaningSearchHit.toCanonicalRecallResult(): CanonicalRecallResult {
    val page = rankedPdfPageNumber ?: citedPdfPageNumber
    return CanonicalRecallResult(
        memoryId = memoryId,
        revisionId = revisionId,
        sourceId = sourceId,
        sourceAssetKey = sourceAssetKey,
        assetType = assetType,
        label = label,
        excerpt = summaryText,
        evidenceId = null,
        locator = page?.let { EvidenceLocator(PdfPageEvidenceLocator.formatLocator(it)) },
        retrievalPath = CanonicalRecallRetrievalPath.MEANING,
        rankScore = score,
        openPageNumber = page,
        evidenceTokenBoosted = evidenceTokenBoosted,
    )
}
