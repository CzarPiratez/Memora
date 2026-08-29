package com.memora.app.application.images

import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport

/**
 * UI-facing screenshot keyword hit after MIG-07 cutover.
 *
 * Mapped from MemoryEvidence search hits; not read from ScreenshotOcrExtractionDao.
 * Screenshots have no PDF page — open-original uses source identity only.
 */
data class ScreenshotOcrKeywordSearchHit(
    val label: String,
    val excerpt: String,
    val sourceId: String,
    val sourceAssetKey: String,
) {
    init {
        require(label.isNotBlank()) { "A screenshot OCR search hit needs a label." }
        require(excerpt.isNotBlank()) { "A screenshot OCR search hit needs an excerpt." }
        require(sourceId.isNotBlank()) { "A screenshot OCR search hit needs a source id." }
        require(sourceAssetKey.isNotBlank()) {
            "A screenshot OCR search hit needs a source asset key."
        }
    }
}

sealed interface ScreenshotOcrKeywordSearchOutcome {
    data object BlankQuery : ScreenshotOcrKeywordSearchOutcome

    /** Normalized query was submitted, but no READY screenshot Memory evidence exists. */
    data class NothingSavedToSearch(
        val query: String,
    ) : ScreenshotOcrKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved outcome needs the submitted search query."
            }
        }
    }

    data class Matches(
        val query: String,
        val hits: List<ScreenshotOcrKeywordSearchHit>,
        val limitReached: Boolean,
    ) : ScreenshotOcrKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "A screenshot OCR keyword search match list needs the query."
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
