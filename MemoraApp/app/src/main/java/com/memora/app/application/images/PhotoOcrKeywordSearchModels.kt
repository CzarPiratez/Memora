package com.memora.app.application.images

import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport

/**
 * UI-facing photo keyword hit after MIG-07 cutover.
 *
 * Mapped from MemoryEvidence search hits; not read from PhotoOcrExtractionDao.
 * Photos have no PDF page — open-original uses source identity only.
 */
data class PhotoOcrKeywordSearchHit(
    val label: String,
    val excerpt: String,
    val sourceId: String,
    val sourceAssetKey: String,
) {
    init {
        require(label.isNotBlank()) { "A photo OCR search hit needs a label." }
        require(excerpt.isNotBlank()) { "A photo OCR search hit needs an excerpt." }
        require(sourceId.isNotBlank()) { "A photo OCR search hit needs a source id." }
        require(sourceAssetKey.isNotBlank()) {
            "A photo OCR search hit needs a source asset key."
        }
    }
}

sealed interface PhotoOcrKeywordSearchOutcome {
    data object BlankQuery : PhotoOcrKeywordSearchOutcome

    /** Normalized query was submitted, but no READY photo Memory evidence exists. */
    data class NothingSavedToSearch(
        val query: String,
    ) : PhotoOcrKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved outcome needs the submitted search query."
            }
        }
    }

    data class Matches(
        val query: String,
        val hits: List<PhotoOcrKeywordSearchHit>,
        val limitReached: Boolean,
    ) : PhotoOcrKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "A photo OCR keyword search match list needs the query."
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
