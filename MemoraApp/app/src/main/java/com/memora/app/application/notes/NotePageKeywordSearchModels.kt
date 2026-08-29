package com.memora.app.application.notes

import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport

/**
 * UI-facing note keyword hit after MIG-07 cutover.
 *
 * Mapped from MemoryEvidence search hits; not read from NotePageExtractionDao.
 * Open-original remains source-identity based (OneNote web/client URLs).
 */
data class NotePageKeywordSearchHit(
    val label: String,
    val excerpt: String,
    val sourceId: String,
    val sourceAssetKey: String,
) {
    init {
        require(label.isNotBlank()) { "A note search hit needs a label." }
        require(excerpt.isNotBlank()) { "A note search hit needs an excerpt." }
        require(sourceId.isNotBlank()) { "A note search hit needs a source id." }
        require(sourceAssetKey.isNotBlank()) {
            "A note search hit needs a source asset key."
        }
    }
}

sealed interface NotePageKeywordSearchOutcome {
    data object BlankQuery : NotePageKeywordSearchOutcome

    /** Normalized query was submitted, but no READY note Memory evidence exists. */
    data class NothingSavedToSearch(
        val query: String,
    ) : NotePageKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved outcome needs the submitted search query."
            }
        }
    }

    data class Matches(
        val query: String,
        val hits: List<NotePageKeywordSearchHit>,
        val limitReached: Boolean,
    ) : NotePageKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "A note keyword search match list needs the query."
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
