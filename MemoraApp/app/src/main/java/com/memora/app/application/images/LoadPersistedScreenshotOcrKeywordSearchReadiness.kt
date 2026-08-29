package com.memora.app.application.images

import com.memora.app.application.memory.MemoryEvidenceExcerptSearch
import com.memora.app.application.memory.SearchMemoryEvidence
import com.memora.app.domain.asset.AssetType
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads how much READY screenshot Memory evidence is available for keyword Find.
 *
 * MIG-07 readiness source: Memory evidence substrate (not raw
 * ScreenshotOcrExtractionDao searchable corpus). Honest inventory only — does
 * not claim meaning recall. [screenshotCount] is distinct screenshot assets
 * with READY Memory evidence (not raw OCR extraction rows).
 */
class LoadPersistedScreenshotOcrKeywordSearchReadiness @Inject constructor(
    private val excerptSearch: MemoryEvidenceExcerptSearch,
) {
    suspend operator fun invoke(): ScreenshotOcrKeywordSearchReadiness =
        withContext(Dispatchers.IO) {
            val counts = excerptSearch.countCurrentReadyEvidenceCorpus(
                assemblySchemaVersion = SearchMemoryEvidence.DEFAULT_ASSEMBLY_SCHEMA,
                assetType = AssetType.SCREENSHOT,
            )
            ScreenshotOcrKeywordSearchReadiness(screenshotCount = counts.documentCount)
        }
}

data class ScreenshotOcrKeywordSearchReadiness(
    val screenshotCount: Int,
) {
    init {
        require(screenshotCount >= 0) { "Screenshot count cannot be negative." }
    }

    val isEmpty: Boolean get() = screenshotCount == 0
}
