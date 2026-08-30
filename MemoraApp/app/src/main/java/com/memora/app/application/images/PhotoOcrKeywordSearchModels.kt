package com.memora.app.application.images

import com.memora.app.application.memory.CanonicalRecallResult
import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport

/**
 * UI-facing photo keyword hit after MIG-07 cutover.
 */
data class PhotoOcrKeywordSearchHit(
    val recall: CanonicalRecallResult,
) {
    val label: String get() = recall.label
    val excerpt: String get() = recall.excerpt
    val sourceId: String get() = recall.sourceId.value
    val sourceAssetKey: String get() = recall.sourceAssetKey.value
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
