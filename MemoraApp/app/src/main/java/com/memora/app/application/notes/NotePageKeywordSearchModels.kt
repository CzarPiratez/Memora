package com.memora.app.application.notes

import com.memora.app.application.memory.CanonicalRecallResult
import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport

/**
 * UI-facing note keyword hit after MIG-07 cutover.
 *
 * Mapped from MemoryEvidence search hits; not read from NotePageExtractionDao.
 * Open-original remains source-identity based (OneNote web/client URLs).
 */
data class NotePageKeywordSearchHit(
    val recall: CanonicalRecallResult,
) {
    val label: String get() = recall.label
    val excerpt: String get() = recall.excerpt
    val sourceId: String get() = recall.sourceId.value
    val sourceAssetKey: String get() = recall.sourceAssetKey.value
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
