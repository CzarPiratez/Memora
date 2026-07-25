package com.memora.app.application.documents

import com.memora.app.data.local.MemoraDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Searches already-persisted PDF page text on this phone.
 *
 * Keyword / substring only. Does not reopen PDFs, invoke AI, use the network, or
 * claim meaning-based Memory recall.
 */
class SearchPersistedPdfPageText @Inject constructor(
    private val database: MemoraDatabase,
) {
    suspend operator fun invoke(rawQuery: String): PdfKeywordSearchOutcome =
        withContext(Dispatchers.IO) {
            val query = PdfKeywordSearchSupport.normalizeQuery(rawQuery)
                ?: return@withContext PdfKeywordSearchOutcome.BlankQuery

            val rows = database.pdfExtractionDao().searchCurrentPages(
                escapedNeedle = PdfKeywordSearchSupport.escapeForLike(query),
                schemaVersion = PdfKeywordSearchSupport.SCHEMA_VERSION,
                limit = PdfKeywordSearchSupport.MAX_RESULTS,
            )

            val hits = rows.map { row ->
                PdfKeywordSearchHit(
                    label = row.displayName?.takeIf { it.isNotBlank() }
                        ?: row.title?.takeIf { it.isNotBlank() }
                        ?: "PDF document",
                    pageNumber = row.pageNumber,
                    excerpt = PdfKeywordSearchSupport.excerptAroundMatch(row.pageText, query),
                    sourceId = row.sourceId,
                    sourceAssetKey = row.sourceAssetKey,
                )
            }

            PdfKeywordSearchOutcome.Matches(query = query, hits = hits)
        }
}

data class PdfKeywordSearchHit(
    val label: String,
    val pageNumber: Int,
    val excerpt: String,
    val sourceId: String,
    val sourceAssetKey: String,
) {
    init {
        require(label.isNotBlank()) { "A PDF search hit needs a label." }
        require(pageNumber > 0) { "A PDF search hit page number must be positive." }
        require(excerpt.isNotBlank()) { "A PDF search hit needs an excerpt." }
        require(sourceId.isNotBlank()) { "A PDF search hit needs a source id." }
        require(sourceAssetKey.isNotBlank()) { "A PDF search hit needs a source asset key." }
    }
}

sealed interface PdfKeywordSearchOutcome {
    data object BlankQuery : PdfKeywordSearchOutcome

    data class Matches(
        val query: String,
        val hits: List<PdfKeywordSearchHit>,
    ) : PdfKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) { "A PDF keyword search match list needs the query." }
        }
    }
}
