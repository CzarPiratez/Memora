package com.memora.app.application.documents

import com.memora.app.application.memory.MemoryEvidenceExcerptSearch
import com.memora.app.application.memory.SearchMemoryEvidence
import com.memora.app.domain.asset.AssetType
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads how much READY PDF Memory evidence is available for keyword Find.
 *
 * MIG-07 readiness source: Memory evidence substrate (not raw PdfExtractionDao
 * searchable corpus). Honest inventory only — does not claim meaning recall.
 */
class LoadPersistedPdfKeywordSearchReadiness @Inject constructor(
    private val excerptSearch: MemoryEvidenceExcerptSearch,
) {
    suspend operator fun invoke(): PdfKeywordSearchReadiness =
        withContext(Dispatchers.IO) {
            val counts = excerptSearch.countCurrentReadyEvidenceCorpus(
                assemblySchemaVersion = SearchMemoryEvidence.DEFAULT_ASSEMBLY_SCHEMA,
                assetType = AssetType.PDF,
            )
            PdfKeywordSearchReadiness(
                pageCount = counts.evidenceCount,
                documentCount = counts.documentCount,
            )
        }
}

data class PdfKeywordSearchReadiness(
    val pageCount: Int,
    val documentCount: Int,
) {
    init {
        require(pageCount >= 0) { "Page count cannot be negative." }
        require(documentCount >= 0) { "Document count cannot be negative." }
        if (pageCount == 0) {
            require(documentCount == 0) {
                "Empty page corpus cannot report documents."
            }
        } else {
            require(documentCount > 0) {
                "Non-empty page corpus needs at least one document."
            }
        }
    }

    val isEmpty: Boolean get() = pageCount == 0
}
