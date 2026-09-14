package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchReadiness
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.intelligence.MeaningRecallCue

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
            "on this phone — PDFs, photos, screenshots, and notes once each has a meaning " +
            "index. Use keyword Find for exact words. Your memories never leave this phone " +
            "for search."

    const val QUERY_LABEL = "A short recall cue"

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

    /**
     * Names what UNFYND actually looked for.
     *
     * The old copy said nothing was "close enough with the on-device meaning
     * model", which blamed the meaning model for a decision the lexical gate had
     * made — on device the model ranked the right PDFs and the gate removed them.
     * Since a partial tier now answers whenever any named word was found
     * (D-12), reaching this state means no saved text carried a single one.
     */
    fun noMatchesBody(query: String): String {
        require(query.isNotBlank())
        val named = MeaningRecallCue.contentTokens(query)
        if (named.isEmpty()) {
            return "That didn't name anything from a file. Try a word you remember seeing, " +
                "like silky or timetable."
        }
        return "Nothing saved on this phone mentions ${quoteWords(named)}. " +
            "Try a word you remember seeing inside the file, or use keyword Find."
    }

    /**
     * Shown when no saved Memory carried every word the person used (D-12). It
     * must lead with what is *missing*, so a partial list is never mistaken for
     * a complete one.
     */
    fun partialMatchBody(matched: List<String>, missing: List<String>): String {
        require(matched.isNotEmpty())
        require(missing.isNotEmpty())
        return "Closest files mention ${quoteWords(matched)}. " +
            "Nothing saved says ${quoteWords(missing)}."
    }

    /**
     * Zero-overlap paraphrase (meaning-only tier). Must lead with the miss so
     * a cosine neighbour is never mistaken for a word hit, and must not claim
     * two words mean the same thing.
     *
     * [missing] comes from [com.memora.app.domain.intelligence.RecallPrecision.MeaningOnly],
     * not from re-reading the query here: the banner has to name the same words
     * the gate actually required.
     */
    fun meaningOnlyBody(missing: List<String>): String {
        require(missing.isNotEmpty()) { "Meaning-only copy needs the words that were not found." }
        return "Nothing saved on this phone mentions ${quoteWords(missing)}. " +
            "These are the closest by meaning on this phone — not because they " +
            "contain those words."
    }

    /** `a` · `a and b` · `a, b and c` — always quoted, always the person's own words. */
    private fun quoteWords(words: List<String>): String {
        val quoted = words.map { "\"$it\"" }
        return when (quoted.size) {
            1 -> quoted.first()
            else -> quoted.dropLast(1).joinToString(", ") + " and " + quoted.last()
        }
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
        CanonicalRecallWhyCopy.whyThisResult(hit, query)

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
