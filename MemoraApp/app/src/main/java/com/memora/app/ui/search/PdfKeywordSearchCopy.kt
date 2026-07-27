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

    const val SEARCHING_BODY =
        "Searching saved PDF text on this phone…"

    const val NO_MATCHES_BODY =
        "No saved PDF page text on this phone matched those words. " +
            "Try different words, or finish Local PDF reading for a document first."

    fun noMatchesBody(query: String): String {
        require(query.isNotBlank()) { "No-matches copy needs the submitted query." }
        return "No saved PDF page text on this phone matched \"$query\". " +
            "Try different words, or finish Local PDF reading for a document first."
    }

    const val NOTHING_SAVED_BODY =
        "Nothing is saved for search yet. Finish Local PDF reading for a connected " +
            "folder first. Memora only searches PDF text already saved on this phone — " +
            "this is keyword matching, not meaning-based recall."

    const val READINESS_LOADING_BODY = "Checking saved PDF text on this phone…"

    const val READINESS_COULD_NOT_LOAD_BODY =
        "Could not check how much PDF text is saved for search on this phone. " +
            "Try opening this screen again in a moment."

    /**
     * Honest inventory of current-fingerprint pages ready for keyword search.
     *
     * Does not claim Memory, meaning, or that discovery alone is searchable.
     */
    fun readinessBody(pageCount: Int, documentCount: Int): String {
        require(pageCount >= 0) { "Readiness page count cannot be negative." }
        require(documentCount >= 0) { "Readiness document count cannot be negative." }
        if (pageCount == 0) {
            require(documentCount == 0) {
                "Empty readiness cannot report documents."
            }
            return "Nothing is saved for keyword search yet. " +
                "Finish Local PDF reading for a connected folder first."
        }
        require(documentCount > 0) {
            "Non-empty readiness needs at least one document."
        }

        val pages = if (pageCount == 1) "1 saved page" else "$pageCount saved pages"
        val documents = if (documentCount == 1) {
            "1 PDF"
        } else {
            "$documentCount PDFs"
        }
        val verb = if (pageCount == 1) "is" else "are"
        return "$pages from $documents $verb ready for keyword search on this phone. " +
            "This is keyword matching, not meaning-based recall."
    }

    const val SEARCH_COULD_NOT_FINISH_BODY =
        "Search could not finish on this phone. Try again in a moment. " +
            "Memora does not reopen your original files for this search."

    const val RESULTS_HINT =
        "Matches show the page and a short excerpt from the saved text. " +
            "Open Why this result? to see the matching evidence."

    /** Hard display cap for interim keyword search; must match search support. */
    const val MAX_LISTED_MATCHES = 20

    /**
     * Honest results summary: submitted query, listed count, optional cap, then hint.
     *
     * When [limitReached] is true, Memora may have stopped early at
     * [MAX_LISTED_MATCHES]; it does not claim a total corpus match count.
     */
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
                "more saved pages may also contain these words."
        } else {
            ""
        }
        return forLine + countLine + capLine + " " + RESULTS_HINT
    }

    const val WHY_THIS_RESULT_LABEL = "Why this result?"

    const val HIDE_WHY_LABEL = "Hide explanation"

    const val BACK_LABEL = "Back"

    fun pageLabel(pageNumber: Int): String = "Page $pageNumber"

    /**
     * Builds a citation from stored hit fields and the search query only.
     *
     * [documentLabel] is the same saved display name shown on the result card.
     * Does not invent confidence, meaning, or AI reasons, and does not reopen files.
     */
    fun whyThisResultBody(
        query: String,
        documentLabel: String,
        pageNumber: Int,
        excerpt: String,
    ): String {
        require(query.isNotBlank()) { "Why this result needs the search query." }
        require(documentLabel.isNotBlank()) {
            "Why this result needs the saved document label."
        }
        require(pageNumber > 0) { "Why this result needs a positive page number." }
        require(excerpt.isNotBlank()) { "Why this result needs a stored excerpt." }

        return "Memora matched \"$query\" in saved PDF page text from " +
            "\"$documentLabel\" on this phone (${pageLabel(pageNumber)}). " +
            "Matching evidence: $excerpt. " +
            "This is keyword matching, not meaning-based recall."
    }

    const val OPEN_ORIGINAL_PDF_LABEL = "Open original PDF"

    const val OPEN_ORIGINAL_PDF_HINT =
        "Opens a read-only preview of this page inside Memora. " +
            "Your file stays where it is; Memora does not edit it."

    const val OPEN_FEEDBACK_OPENING_BODY = "Opening a read-only preview of that page…"

    const val OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY =
        "Memora cannot open that original file right now. Reconnect the PDF folder " +
            "if access was removed, then try again."

    const val OPEN_FEEDBACK_COULD_NOT_OPEN_BODY =
        "Memora could not render that PDF page. Try again, or reconnect the folder."

    const val DISMISS_OPEN_FEEDBACK_LABEL = "Dismiss"

    const val PREVIEW_TITLE = "Original PDF page"

    const val CLOSE_PREVIEW_LABEL = "Back to results"

    const val PREVIEW_SCOPE_BODY =
        "This is a read-only preview of the page Memora cited. " +
            "Search still used saved text on this phone — not a live re-read of the whole file."

    fun previewPageCaption(pageNumber: Int, pageCount: Int): String {
        require(pageNumber > 0 && pageCount > 0 && pageNumber <= pageCount)
        return "Page $pageNumber of $pageCount"
    }
}
