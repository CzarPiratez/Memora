package com.memora.app.application.documents

import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.MemoraDatabaseHandle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Searches already-persisted PDF page text on this phone.
 *
 * Keyword / substring only. Does not reopen PDFs, invoke AI, use the network, or
 * claim meaning-based Memory recall.
 *
 * Always resolves the live database through [database] so a clear/reopen cannot
 * leave this use case bound to a closed Room instance.
 */
class SearchPersistedPdfPageText(
    private val database: () -> MemoraDatabase,
) {
    @Inject
    constructor(
        databaseHandle: MemoraDatabaseHandle,
    ) : this(
        database = { databaseHandle.database() },
    )

    /** Test helper bound to one in-memory / fixture database. */
    constructor(database: MemoraDatabase) : this(database = { database })

    suspend operator fun invoke(rawQuery: String): PdfKeywordSearchOutcome =
        withContext(Dispatchers.IO) {
            val query = PdfKeywordSearchSupport.normalizeQuery(rawQuery)
                ?: return@withContext PdfKeywordSearchOutcome.BlankQuery

            val dao = database().pdfExtractionDao()
            if (dao.countCurrentSearchablePages(PdfKeywordSearchSupport.SCHEMA_VERSION) == 0) {
                return@withContext PdfKeywordSearchOutcome.NothingSavedToSearch(query = query)
            }

            val rows = dao.searchCurrentPages(
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

            PdfKeywordSearchOutcome.Matches(
                query = query,
                hits = hits,
                limitReached = hits.size >= PdfKeywordSearchSupport.MAX_RESULTS,
            )
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

    /** Normalized query was submitted, but no current searchable PDF page text exists. */
    data class NothingSavedToSearch(
        val query: String,
    ) : PdfKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved outcome needs the submitted search query."
            }
        }
    }

    data class Matches(
        val query: String,
        val hits: List<PdfKeywordSearchHit>,
        val limitReached: Boolean,
    ) : PdfKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) { "A PDF keyword search match list needs the query." }
            if (limitReached) {
                require(hits.size >= PdfKeywordSearchSupport.MAX_RESULTS) {
                    "limitReached requires a full result page of ${PdfKeywordSearchSupport.MAX_RESULTS}."
                }
            }
        }
    }
}
