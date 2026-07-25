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

    const val RESULTS_HINT = "Matches show the page and a short excerpt from the saved text."

    const val BACK_LABEL = "Back"

    fun pageLabel(pageNumber: Int): String = "Page $pageNumber"
}
