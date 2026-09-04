package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchReadiness
import com.memora.app.application.memory.toCanonicalRecallResult
import com.memora.app.domain.asset.AssetType

/**
 * Plain-language copy for candidate Find-by-meaning (E5b2 / ADR-031/032).
 *
 * Must not claim measured Local Intelligence AVAILABLE / midrange SLAs.
 */
object MeaningSearchCopy {
    const val ENTRY_LABEL = "Find by meaning"

    const val SCREEN_TITLE = "Find by meaning"

    const val SCOPE_BODY =
        "Search ranks Asset Memories you already built with the on-device meaning model " +
            "on this phone. Use keyword Find for exact words. Your memories never leave " +
            "this phone for search."

    const val QUERY_LABEL = "What are you trying to remember?"

    const val SEARCH_LABEL = "Search by meaning on this phone"

    const val CLEAR_QUERY_LABEL = "Clear"

    const val CANCEL_SEARCH_LABEL = "Cancel search"

    const val BACK_LABEL = "Back"

    const val EMPTY_QUERY_BODY = "Type a short recall cue, then search."

    const val SEARCHING_BODY = "Ranking saved Asset Memories on this phone…"

    const val SEARCH_COULD_NOT_FINISH_BODY =
        "Meaning search could not finish. Try again in a moment."

    const val READINESS_LOADING_BODY = "Checking on-device meaning search on this phone…"

    const val READINESS_COULD_NOT_LOAD_BODY =
        "Could not check meaning-search readiness on this phone. Try opening this screen again."

    fun readinessBody(snapshot: MeaningSearchReadiness.Ready): String {
        val corpus = CorpusHonestyCopy.summaryBody(snapshot.corpusCompleteness)
        if (snapshot.indexedCount == 0 && snapshot.memoriesReadyCount == 0 &&
            snapshot.corpusCompleteness.counts.memoriesPendingAssembly == 0
        ) {
            return "No Asset Memories are ready yet. Build memories from saved facts first, " +
                "then build a meaning index from About on-device meaning search."
        }
        return "$corpus Meaning search on this phone uses the on-device Universal Sentence Encoder."
    }

    fun engineUnavailableBody(reason: String): String {
        require(reason.isNotBlank())
        return "$reason Download the on-device meaning model from About on-device " +
            "meaning search first. Keyword Find buttons still work."
    }

    fun nothingIndexedBody(query: String): String {
        require(query.isNotBlank())
        return "Nothing is indexed for meaning search yet for \"$query\". " +
            "Build Asset Memories, then build the meaning index."
    }

    fun noMatchesBody(query: String): String {
        require(query.isNotBlank())
        return "No indexed Asset Memory was close enough to \"$query\" with the " +
            "on-device meaning model. Try a different cue, or use keyword Find."
    }

    fun limitReachedBody(): String =
        "Showing the strongest matches for your cue. Add another word if you need a tighter list."

    fun hitTypeLabel(type: AssetType): String = when (type) {
        AssetType.PDF -> "PDF memory"
        AssetType.SCREENSHOT -> "Screenshot memory"
        AssetType.PHOTO -> "Photo memory"
        AssetType.NOTE -> "Note memory"
    }

    fun citedPdfPageLabel(pageNumber: Int): String {
        require(pageNumber > 0)
        return "Cites page $pageNumber"
    }

    fun whyThisResult(hit: MeaningSearchHit, query: String): String =
        CanonicalRecallWhyCopy.whyThisResult(hit.toCanonicalRecallResult(), query)

    fun friendlyHitLabel(label: String): String =
        CanonicalRecallWhyCopy.friendlyDisplayLabel(label)

    const val OPEN_ORIGINAL_LABEL = "Open original"

    const val OPEN_ORIGINAL_HINT =
        "Opens the original file this memory cites. The file stays where it is on this phone."

    fun openOriginalPdfHint(
        citedPdfPageNumber: Int?,
        rankedPdfPageNumber: Int? = null,
    ): String {
        val page = rankedPdfPageNumber ?: citedPdfPageNumber
        return if (page != null) {
            "$OPEN_ORIGINAL_HINT Opens near page $page."
        } else {
            OPEN_ORIGINAL_HINT
        }
    }

    fun rankedPdfPageLabel(pageNumber: Int): String {
        require(pageNumber > 0)
        return "Matched page $pageNumber"
    }

    const val OPEN_FEEDBACK_OPENING_BODY = "Opening the original on this phone…"

    const val OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY =
        "UNFYND could not reopen that original. Access may have been revoked, or the " +
            "file is no longer reachable. Meaning search still uses indexed memories " +
            "saved on this phone."

    const val OPEN_FEEDBACK_COULD_NOT_OPEN_BODY =
        "UNFYND could not open that original. Try again in a moment. Meaning search " +
            "still uses indexed memories saved on this phone."

    const val DISMISS_OPEN_FEEDBACK_LABEL = "Dismiss"
}
