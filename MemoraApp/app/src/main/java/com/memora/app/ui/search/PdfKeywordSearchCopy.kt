package com.memora.app.ui.search

/**
 * Plain-language copy for interim keyword search over saved PDF text.
 *
 * Must not claim meaning-based Memory recall, AI understanding, or cloud search.
 */
object PdfKeywordSearchCopy {
    const val SCREEN_TITLE = "Find saved PDF text"

    const val SCOPE_BODY =
        "Search looks for exact words in PDF text already saved on this phone. " +
            "This is keyword matching, not meaning-based recall yet. " +
            "Memora does not reopen your original files for this search."

    const val QUERY_LABEL = "Words to find"

    const val SEARCH_LABEL = "Search on this phone"

    const val EMPTY_QUERY_BODY = "Type a word or short phrase, then search."

    const val NO_MATCHES_BODY =
        "No saved PDF page text on this phone matched those words. " +
            "Try different words, or finish Local PDF reading for a document first."

    const val RESULTS_HINT =
        "Matches show the page and a short excerpt from the saved text. " +
            "Open Why this result? to see the matching evidence."

    /** Hard display cap for interim keyword search; must match search support. */
    const val MAX_LISTED_MATCHES = 20

    /**
     * Honest results summary: listed count, optional cap disclosure, then hint.
     *
     * When [limitReached] is true, Memora may have stopped early at
     * [MAX_LISTED_MATCHES]; it does not claim a total corpus match count.
     */
    fun resultsSummary(matchCount: Int, limitReached: Boolean): String {
        require(matchCount > 0) { "Results summary needs at least one match." }
        if (limitReached) {
            require(matchCount >= MAX_LISTED_MATCHES) {
                "A capped summary needs at least $MAX_LISTED_MATCHES listed matches."
            }
        }

        val countLine = if (matchCount == 1) {
            "Showing 1 match."
        } else {
            "Showing $matchCount matches."
        }
        val capLine = if (limitReached) {
            " Memora lists at most $MAX_LISTED_MATCHES matches for now; " +
                "more saved pages may also contain these words."
        } else {
            ""
        }
        return countLine + capLine + " " + RESULTS_HINT
    }

    const val WHY_THIS_RESULT_LABEL = "Why this result?"

    const val HIDE_WHY_LABEL = "Hide explanation"

    const val BACK_LABEL = "Back"

    fun pageLabel(pageNumber: Int): String = "Page $pageNumber"

    /**
     * Builds a citation from stored hit fields and the search query only.
     *
     * Does not invent confidence, meaning, or AI reasons.
     */
    fun whyThisResultBody(query: String, pageNumber: Int, excerpt: String): String {
        require(query.isNotBlank()) { "Why this result needs the search query." }
        require(pageNumber > 0) { "Why this result needs a positive page number." }
        require(excerpt.isNotBlank()) { "Why this result needs a stored excerpt." }

        return "Memora matched \"$query\" in saved PDF page text on this phone " +
            "(${pageLabel(pageNumber)}). Matching evidence: $excerpt. " +
            "This is keyword matching, not meaning-based recall."
    }
}
