package com.memora.app.application.documents

import com.memora.app.application.memory.CanonicalRecallResult
import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport

/**
 * UI-facing PDF keyword hit after MIG-07 cutover.
 *
 * Mapped from MemoryEvidence search hits; not read from PdfExtractionDao.
 */
data class PdfKeywordSearchHit(
    val recall: CanonicalRecallResult,
) {
    val label: String get() = recall.label
    val pageNumber: Int
        get() = recall.openPageNumber
            ?: error("PDF keyword hit requires a positive open page number.")
    val excerpt: String get() = recall.excerpt
    val sourceId: String get() = recall.sourceId.value
    val sourceAssetKey: String get() = recall.sourceAssetKey.value

    init {
        require(recall.openPageNumber != null && recall.openPageNumber > 0) {
            "A PDF search hit page number must be positive."
        }
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
