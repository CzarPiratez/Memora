package com.memora.app.application.images

import com.memora.app.application.memory.MemoryEvidenceExcerptSearch
import com.memora.app.application.memory.SearchMemoryEvidence
import com.memora.app.domain.asset.AssetType
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads how much READY photo Memory evidence is available for keyword Find.
 *
 * MIG-07 readiness source: Memory evidence substrate (not raw
 * PhotoOcrExtractionDao searchable corpus). Honest inventory only — does
 * not claim meaning recall. [photoCount] is distinct photo assets
 * with READY Memory evidence (not raw OCR extraction rows).
 */
class LoadPersistedPhotoOcrKeywordSearchReadiness @Inject constructor(
    private val excerptSearch: MemoryEvidenceExcerptSearch,
) {
    suspend operator fun invoke(): PhotoOcrKeywordSearchReadiness =
        withContext(Dispatchers.IO) {
            val counts = excerptSearch.countCurrentReadyEvidenceCorpus(
                assemblySchemaVersion = SearchMemoryEvidence.DEFAULT_ASSEMBLY_SCHEMA,
                assetType = AssetType.PHOTO,
            )
            PhotoOcrKeywordSearchReadiness(photoCount = counts.documentCount)
        }
}

data class PhotoOcrKeywordSearchReadiness(
    val photoCount: Int,
) {
    init {
        require(photoCount >= 0) { "Photo count cannot be negative." }
    }

    val isEmpty: Boolean get() = photoCount == 0
}
