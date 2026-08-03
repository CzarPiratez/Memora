package com.memora.app.data.local

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.SavedPdfPageText
import com.memora.app.domain.extraction.SavedPdfPageTextSource

class RoomSavedPdfPageTextSource(
    private val assetRepository: AssetRepository,
    private val database: () -> MemoraDatabase,
) : SavedPdfPageTextSource {
    override suspend fun listCurrentVerifiedPages(
        sourceId: String,
        sourceAssetKey: String,
    ): List<SavedPdfPageText> {
        if (sourceId.isBlank() || sourceAssetKey.isBlank()) return emptyList()
        val record = assetRepository.find(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        ) ?: return emptyList()
        val asset = record.asset
        if (asset.type != AssetType.PDF) return emptyList()
        return database().assetMemoryFactDao().findPdfPages(
            sourceId = sourceId,
            sourceAssetKey = sourceAssetKey,
            fingerprint = asset.fingerprint.value,
            schemaVersion = PDF_SCHEMA,
        ).mapNotNull { page ->
            val text = page.pageText.trim()
            if (text.isEmpty()) null else SavedPdfPageText(page.pageNumber, text)
        }
    }

    private companion object {
        const val PDF_SCHEMA = "pdf-extraction-v1"
    }
}
