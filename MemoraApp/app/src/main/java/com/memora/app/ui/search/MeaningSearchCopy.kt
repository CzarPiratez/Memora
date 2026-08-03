package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchReadiness
import com.memora.app.domain.asset.AssetType

/**
 * Plain-language copy for candidate Find-by-meaning (E5b2 / ADR-031).
 *
 * Must not claim measured Local Intelligence AVAILABLE / midrange SLAs.
 */
object MeaningSearchCopy {
    const val ENTRY_LABEL = "Find by meaning"

    const val SCREEN_TITLE = "Find by meaning"

    const val SCOPE_BODY =
        "Search ranks Asset Memories you already built, using the on-device meaning " +
            "model on this phone. This is a compact candidate ranker — not a claim that " +
            "full measured meaning search is finished. Keyword Find buttons still search " +
            "exact words. Memora does not upload your memories for this search."

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
        if (snapshot.indexedCount == 0) {
            return if (snapshot.memoriesReadyCount == 0) {
                "No Asset Memories are ready yet. Build memories from saved facts first, " +
                    "then build a meaning index from About on-device meaning search."
            } else {
                "Asset Memories are ready, but the meaning index is empty. " +
                    "Open About on-device meaning search and build the meaning index."
            }
        }
        val indexed = if (snapshot.indexedCount == 1) {
            "1 indexed memory"
        } else {
            "${snapshot.indexedCount} indexed memories"
        }
        return "$indexed ready for candidate meaning search on this phone " +
            "(compact on-device model). This is not a measured AVAILABLE claim."
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
            "compact on-device model. Try a different cue, or use keyword Find."
    }

    fun limitReachedBody(): String =
        "Showing the top ${10} closest memories. Narrow your cue for a tighter list."

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

    fun whyThisResult(hit: MeaningSearchHit, query: String): String {
        require(query.isNotBlank())
        val excerpt = hit.summaryText.trim().replace(Regex("\\s+"), " ")
        val clipped = if (excerpt.length <= 160) excerpt else excerpt.take(157) + "…"
        val pageCite = when {
            hit.rankedPdfPageNumber != null ->
                " It matched indexed PDF page ${hit.rankedPdfPageNumber}."
            hit.citedPdfPageNumber != null ->
                " It cites page ${hit.citedPdfPageNumber} of the PDF."
            else -> ""
        }
        val boostNote = if (hit.evidenceTokenBoosted) {
            " Rank also rose because your cue appears in this saved evidence text " +
                "(disclosed assist — still candidate meaning, not keyword Find alone)."
        } else {
            ""
        }
        return "Why this result? Your cue \"$query\" ranked closest to this saved " +
            "Asset Memory evidence: \"$clipped\".$pageCite$boostNote Score is " +
            "candidate cosine similarity on this phone — not a guarantee of full " +
            "meaning match."
    }

    const val OPEN_ORIGINAL_LABEL = "Open original"

    const val OPEN_ORIGINAL_HINT =
        "Opens the original file Memora cited for this memory. Search still used the " +
            "on-device meaning index — not a live re-read for ranking."

    fun openOriginalPdfHint(
        citedPdfPageNumber: Int?,
        rankedPdfPageNumber: Int? = null,
    ): String {
        if (rankedPdfPageNumber != null) {
            return "$OPEN_ORIGINAL_HINT Opens ranked PDF page $rankedPdfPageNumber " +
                "from the meaning index (candidate page ranking — not measured AVAILABLE)."
        }
        val citeBit = if (citedPdfPageNumber != null) {
            "Memory cite is page $citedPdfPageNumber."
        } else {
            "No Memory page cite — page 1 is the fallback if cue-best cannot run."
        }
        return "$OPEN_ORIGINAL_HINT On Open, Memora may pick a better matching " +
            "saved page for your cue with the on-device model ($citeBit)."
    }

    fun rankedPdfPageLabel(pageNumber: Int): String {
        require(pageNumber > 0)
        return "Matched page $pageNumber"
    }

    const val OPEN_FEEDBACK_OPENING_BODY = "Opening the original on this phone…"

    const val OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY =
        "Memora could not reopen that original. Access may have been revoked, or the " +
            "file is no longer reachable. Meaning search still uses indexed memories " +
            "saved on this phone."

    const val OPEN_FEEDBACK_COULD_NOT_OPEN_BODY =
        "Memora could not open that original. Try again in a moment. Meaning search " +
            "still uses indexed memories saved on this phone."

    const val DISMISS_OPEN_FEEDBACK_LABEL = "Dismiss"
}
