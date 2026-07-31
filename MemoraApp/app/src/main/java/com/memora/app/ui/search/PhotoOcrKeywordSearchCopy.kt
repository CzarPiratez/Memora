package com.memora.app.ui.search

object PhotoOcrKeywordSearchCopy {
    const val SCREEN_TITLE = "Find saved photo text"
    const val SCOPE_BODY =
        "Search looks for exact words in photo text already saved on this phone. " +
            "This is keyword matching, not meaning-based recall. Memora does not " +
            "reopen original photos during search."
    const val QUERY_LABEL = "Words to find"
    const val SEARCH_LABEL = "Search on this phone"
    const val CLEAR_QUERY_LABEL = "Clear"
    const val CANCEL_SEARCH_LABEL = "Cancel search"
    const val EMPTY_QUERY_BODY = "Type a word or short phrase, then search."
    const val SEARCHING_BODY = "Searching saved photo text on this phone…"
    const val NOTHING_SAVED_BODY =
        "Nothing is saved for search yet. Finish Read text from photos in photo setup first."
    const val SEARCH_COULD_NOT_FINISH_BODY = "Search could not finish. Try again in a moment."
    const val WHY_THIS_RESULT_LABEL = "Why this result?"
    const val HIDE_WHY_LABEL = "Hide explanation"
    const val OPEN_ORIGINAL_LABEL = "Open original"
    const val OPENING_BODY = "Opening a read-only preview of that photo…"
    const val SOURCE_UNAVAILABLE_BODY =
        "Memora cannot open that photo right now. Photo access may have changed."
    const val COULD_NOT_OPEN_BODY = "Memora could not show that photo."
    const val DISMISS_LABEL = "Dismiss"
    const val BACK_LABEL = "Back"
    const val PREVIEW_TITLE = "Original photo"
    const val CLOSE_PREVIEW_LABEL = "Back to results"
    const val PREVIEW_SCOPE_BODY =
        "This is a read-only preview. Search used saved OCR text, not a live image search."

    fun noMatchesBody(query: String) = "No saved photo text matched \"$query\"."

    fun readinessBody(photoCount: Int): String =
        if (photoCount == 0) {
            "Nothing is saved for keyword search yet."
        } else {
            "$photoCount ${if (photoCount == 1) "photo is" else "photos are"} ready for keyword search. " +
                "This is keyword matching, not meaning-based recall."
        }

    fun resultsSummary(query: String, count: Int, capped: Boolean): String =
        "Results for \"$query\". Showing $count ${if (count == 1) "match" else "matches"}." +
            if (capped) " Memora lists at most 20 matches." else ""

    fun whyThisResultBody(query: String, label: String, excerpt: String) =
        "Memora matched \"$query\" in saved photo OCR text from \"$label\". " +
            "Matching evidence: $excerpt. This is keyword matching, not meaning-based recall."
}
