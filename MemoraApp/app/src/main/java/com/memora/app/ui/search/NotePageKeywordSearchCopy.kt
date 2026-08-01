package com.memora.app.ui.search

/**
 * Plain-language copy for interim keyword search over saved OneNote page text.
 *
 * Must not claim meaning-based Memory recall, AI understanding, or cloud search.
 */
object NotePageKeywordSearchCopy {
    const val SCREEN_TITLE = "Find saved note text"

    const val ENTRY_LABEL = "Find saved note text"

    const val SCOPE_BODY =
        "Search looks for exact words in OneNote page text already saved on this phone. " +
            "This is keyword matching, not meaning-based recall yet. " +
            "Memora does not call Microsoft or reopen OneNote for this search."

    const val QUERY_LABEL = "Words to find"

    const val SEARCH_LABEL = "Search on this phone"

    const val CLEAR_QUERY_LABEL = "Clear"

    const val CANCEL_SEARCH_LABEL = "Cancel search"

    const val EMPTY_QUERY_BODY = "Type a word or short phrase, then search."

    const val SEARCHING_BODY =
        "Searching saved note text on this phone…"

    fun noMatchesBody(query: String): String {
        require(query.isNotBlank()) { "No-matches copy needs the submitted query." }
        return "No saved note text on this phone matched \"$query\". " +
            "Try different words, or finish Extract OneNote page text in Notes indexing first."
    }

    const val NOTHING_SAVED_BODY =
        "Nothing is saved for search yet. In Notes indexing, Connect OneNote, " +
            "Discover pages, then Extract OneNote page text. Memora only searches note " +
            "text already saved on this phone — this is keyword matching, not meaning-based recall."

    const val READINESS_LOADING_BODY = "Checking saved note text on this phone…"

    const val READINESS_COULD_NOT_LOAD_BODY =
        "Could not check how much note text is saved for search on this phone. " +
            "Try opening this screen again in a moment."

    fun readinessBody(noteCount: Int): String {
        require(noteCount >= 0) { "Readiness note count cannot be negative." }
        if (noteCount == 0) {
            return "Nothing is saved for keyword search yet. " +
                "In Notes indexing, Connect, Discover, then Extract OneNote page text first."
        }
        val items = if (noteCount == 1) "1 note page" else "$noteCount note pages"
        val verb = if (noteCount == 1) "is" else "are"
        return "$items with saved text $verb ready for keyword search on this phone. " +
            "This is keyword matching, not meaning-based recall."
    }

    const val SEARCH_COULD_NOT_FINISH_BODY =
        "Search could not finish on this phone. Try again in a moment. " +
            "Memora does not call Microsoft for this search."

    const val RESULTS_HINT =
        "Matches show the page title and a short excerpt from the saved note text. " +
            "Open Why this result? to see the matching evidence."

    const val MAX_LISTED_MATCHES = 20

    fun resultsSummary(query: String, matchCount: Int, limitReached: Boolean): String {
        require(query.isNotBlank()) { "Results summary needs the submitted query." }
        require(matchCount > 0) { "Results summary needs at least one match." }
        if (limitReached) {
            require(matchCount >= MAX_LISTED_MATCHES) {
                "A capped summary needs at least $MAX_LISTED_MATCHES listed matches."
            }
        }

        val forLine = "Results for \"$query\"."
        val countLine = if (matchCount == 1) {
            " Showing 1 match."
        } else {
            " Showing $matchCount matches."
        }
        val capLine = if (limitReached) {
            " Memora lists at most $MAX_LISTED_MATCHES matches for now; " +
                "more saved notes may also contain these words."
        } else {
            ""
        }
        return forLine + countLine + capLine + " " + RESULTS_HINT
    }

    const val WHY_THIS_RESULT_LABEL = "Why this result?"

    const val HIDE_WHY_LABEL = "Hide explanation"

    const val BACK_LABEL = "Back"

    fun whyThisResultBody(
        query: String,
        noteLabel: String,
        excerpt: String,
    ): String {
        require(query.isNotBlank()) { "Why this result needs the search query." }
        require(noteLabel.isNotBlank()) { "Why this result needs the saved note label." }
        require(excerpt.isNotBlank()) { "Why this result needs a stored excerpt." }

        return "Memora matched \"$query\" in saved OneNote page text from " +
            "\"$noteLabel\" on this phone. " +
            "Matching evidence: $excerpt. " +
            "This is keyword matching, not meaning-based recall."
    }
}
