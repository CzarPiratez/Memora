package com.memora.app.ui.search

/**
 * Plain-language copy for interim keyword search over saved screenshot OCR text.
 *
 * Must not claim meaning-based Memory recall, AI understanding, PHOTO OCR, or cloud.
 */
object ScreenshotOcrKeywordSearchCopy {
    const val SCREEN_TITLE = "Find saved screenshot text"

    const val SCOPE_BODY =
        "Search looks for exact words in screenshot text already saved on this phone. " +
            "This is keyword matching, not meaning-based recall yet. " +
            "Memora does not reopen your original screenshots for this search. " +
            "Ordinary photos are not included."

    const val QUERY_LABEL = "Words to find"

    const val SEARCH_LABEL = "Search on this phone"

    const val CLEAR_QUERY_LABEL = "Clear"

    const val CANCEL_SEARCH_LABEL = "Cancel search"

    const val EMPTY_QUERY_BODY = "Type a word or short phrase, then search."

    const val SEARCHING_BODY =
        "Searching saved screenshot text on this phone…"

    fun noMatchesBody(query: String): String {
        require(query.isNotBlank()) { "No-matches copy needs the submitted query." }
        return "No saved screenshot text on this phone matched \"$query\". " +
            "Try different words, or finish Read text from screenshots first."
    }

    const val NOTHING_SAVED_BODY =
        "Nothing is saved for search yet. Finish Read text from screenshots in " +
            "photo setup first. Memora only searches screenshot OCR text already " +
            "saved on this phone — this is keyword matching, not meaning-based recall."

    const val READINESS_LOADING_BODY = "Checking saved screenshot text on this phone…"

    const val READINESS_COULD_NOT_LOAD_BODY =
        "Could not check how much screenshot text is saved for search on this phone. " +
            "Try opening this screen again in a moment."

    fun readinessBody(screenshotCount: Int): String {
        require(screenshotCount >= 0) { "Readiness screenshot count cannot be negative." }
        if (screenshotCount == 0) {
            return "Nothing is saved for keyword search yet. " +
                "Finish Read text from screenshots in photo setup first."
        }
        val items = if (screenshotCount == 1) {
            "1 screenshot"
        } else {
            "$screenshotCount screenshots"
        }
        val verb = if (screenshotCount == 1) "is" else "are"
        return "$items with saved OCR text $verb ready for keyword search on this phone. " +
            "This is keyword matching, not meaning-based recall."
    }

    const val SEARCH_COULD_NOT_FINISH_BODY =
        "Search could not finish on this phone. Try again in a moment. " +
            "Memora does not reopen your original screenshots for this search."

    const val RESULTS_HINT =
        "Matches show the screenshot name and a short excerpt from the saved OCR text. " +
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
                "more saved screenshots may also contain these words."
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
        screenshotLabel: String,
        excerpt: String,
    ): String {
        require(query.isNotBlank()) { "Why this result needs the search query." }
        require(screenshotLabel.isNotBlank()) {
            "Why this result needs the saved screenshot label."
        }
        require(excerpt.isNotBlank()) { "Why this result needs a stored excerpt." }

        return "Memora matched \"$query\" in saved screenshot OCR text from " +
            "\"$screenshotLabel\" on this phone. " +
            "Matching evidence: $excerpt. " +
            "This is keyword matching, not meaning-based recall."
    }
}
