package com.memora.app.application.memory

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator

/** Shared Canonical Recall fixtures for unit tests. */
object CanonicalRecallTestFixtures {
    fun keywordRecall(
        assetType: AssetType = AssetType.PDF,
        label: String = "fixture.pdf",
        excerpt: String = "meet mira excerpt",
        pageNumber: Int? = if (assetType == AssetType.PDF) 1 else null,
        sourceId: String = "source-1",
        sourceAssetKey: String = "asset-1",
    ): CanonicalRecallResult =
        CanonicalRecallResult(
            memoryId = MemoryId("mem-1"),
            revisionId = MemoryRevisionId("rev-1"),
            sourceId = SourceId(sourceId),
            sourceAssetKey = SourceAssetKey(sourceAssetKey),
            assetType = assetType,
            label = label,
            excerpt = excerpt,
            evidenceId = MemoryEvidenceId("ev-1"),
            locator = pageNumber?.let {
                EvidenceLocator(PdfPageEvidenceLocator.formatLocator(it))
            },
            retrievalPath = CanonicalRecallRetrievalPath.KEYWORD,
            rankScore = null,
            openPageNumber = pageNumber,
        )
}
