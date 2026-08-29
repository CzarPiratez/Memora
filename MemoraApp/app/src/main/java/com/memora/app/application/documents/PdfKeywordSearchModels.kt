package com.memora.app.application.documents

import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport

/**
 * UI-facing PDF keyword hit after MIG-07 cutover.
 *
 * Mapped from MemoryEvidence search hits; not read from PdfExtractionDao.
 */
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

    /** Normalized query was submitted, but no READY PDF Memory evidence exists to search. */
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
            require(query.isNotBlank()) {
                "A PDF keyword search match list needs the query."
            }
            if (limitReached) {
                require(hits.size >= MemoryEvidenceLiteralSearchSupport.MAX_RESULTS) {
                    "limitReached requires a full result page of " +
                        "${MemoryEvidenceLiteralSearchSupport.MAX_RESULTS}."
                }
            }
        }
    }
}
