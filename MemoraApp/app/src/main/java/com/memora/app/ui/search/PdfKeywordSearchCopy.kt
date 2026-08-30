package com.memora.app.ui.search

import com.memora.app.application.documents.PdfKeywordSearchHit
import com.memora.app.application.memory.CanonicalRecallResult

/**
 * Plain-language copy for interim keyword search over saved PDF text.
 *
 * Must not claim meaning-based Memory recall, AI understanding, or cloud search.
 */
object PdfKeywordSearchCopy {
    const val SCREEN_TITLE = "Find saved PDF text"

    const val SCOPE_BODY =
        "Search looks for exact words in PDF Memory evidence already saved on this phone. " +
            "This is keyword matching, not meaning-based recall yet. " +
            "UNFYND does not reopen your original files for this search."

    const val QUERY_LABEL = "Words to find"

    const val SEARCH_LABEL = "Search on this phone"

    const val CLEAR_QUERY_LABEL = "Clear"

    const val CANCEL_SEARCH_LABEL = "Cancel search"

    const val EMPTY_QUERY_BODY = "Type a word or short phrase, then search."

    const val SEARCHING_BODY =
        "Searching saved PDF memory evidence on this phone…"

    const val NO_MATCHES_BODY =
        "No PDF memory evidence on this phone matched those words. " +
            "Try different words, or finish Local PDF reading and Memory assembly first."

    fun noMatchesBody(query: String): String {
        require(query.isNotBlank()) { "No-matches copy needs the submitted query." }
        return "No PDF memory evidence on this phone matched \"$query\". " +
            "Try different words, or finish Local PDF reading and Memory assembly first."
    }

    const val NOTHING_SAVED_BODY =
        "Nothing is ready for PDF keyword search yet. Finish Local PDF reading and " +
            "Memory assembly for a connected folder first. UNFYND only searches READY " +
            "PDF Memory evidence on this phone — this is keyword matching, not " +
            "meaning-based recall."

    const val READINESS_LOADING_BODY = "Checking PDF memory evidence on this phone…"

    const val READINESS_COULD_NOT_LOAD_BODY =
        "Could not check how much PDF memory evidence is ready for search on this phone. " +
            "Try opening this screen again in a moment."

    /**
     * Honest inventory of READY PDF Memory evidence excerpts for keyword search.
     *
     * Counts Memory evidence rows (not raw PdfExtractionDao pages) after MIG-07.
     */
    fun readinessBody(pageCount: Int, documentCount: Int): String {
        require(pageCount >= 0) { "Readiness page count cannot be negative." }
        require(documentCount >= 0) { "Readiness document count cannot be negative." }
        if (pageCount == 0) {
            require(documentCount == 0) {
                "Empty readiness cannot report documents."
            }
            return "Nothing is ready for PDF keyword search yet. " +
                "Finish Local PDF reading and Memory assembly for a connected folder first."
        }
        require(documentCount > 0) {
            "Non-empty readiness needs at least one document."
        }

        val excerpts = if (pageCount == 1) {
            "1 searchable PDF memory excerpt"
        } else {
            "$pageCount searchable PDF memory excerpts"
        }
        val documents = if (documentCount == 1) {
            "1 PDF"
        } else {
            "$documentCount PDFs"
        }
        val verb = if (pageCount == 1) "is" else "are"
        return "$excerpts from $documents $verb ready for keyword search on this phone. " +
            "This is keyword matching, not meaning-based recall."
    }

    const val SEARCH_COULD_NOT_FINISH_BODY =
        "Search could not finish on this phone. Try again in a moment. " +
            "UNFYND does not reopen your original files for this search."

    const val RESULTS_HINT =
        "Matches show the page and a short excerpt from the saved text. " +
            "Open Why this result? to see the matching evidence."

    /** Hard display cap for interim keyword search; must match search support. */
    const val MAX_LISTED_MATCHES = 20

    /**
     * Honest results summary: submitted query, listed count, optional cap, then hint.
     *
     * When [limitReached] is true, UNFYND may have stopped early at
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
            " UNFYND lists at most $MAX_LISTED_MATCHES matches for now; " +
                "more saved pages may also contain these words."
        } else {
            ""
        }
        return forLine + countLine + capLine + " " + RESULTS_HINT
    }

    const val WHY_THIS_RESULT_LABEL = CanonicalRecallWhyCopy.WHY_THIS_RESULT_LABEL

    const val HIDE_WHY_LABEL = "Hide explanation"

    const val BACK_LABEL = "Back"

    fun pageLabel(pageNumber: Int): String = "Page $pageNumber"

    /** Unified Canonical Recall Why dialect (`CANONICAL_RECALL_RESULT_CONTRACT.md`). */
    fun whyThisResultBody(query: String, recall: CanonicalRecallResult): String =
        CanonicalRecallWhyCopy.whyThisResult(recall, query)

    fun whyThisResultBody(hit: PdfKeywordSearchHit, query: String): String =
        whyThisResultBody(query, hit.recall)

    const val OPEN_ORIGINAL_PDF_LABEL = "Open original PDF"

    const val OPEN_ORIGINAL_PDF_HINT =
        "Opens a read-only preview of this page inside UNFYND. " +
            "Your file stays where it is; UNFYND does not edit it."

    const val OPEN_FEEDBACK_OPENING_BODY = "Opening a read-only preview of that page…"

    const val OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY =
        "UNFYND cannot open that original file right now. Reconnect the PDF folder " +
            "if access was removed, then try again."

    const val OPEN_FEEDBACK_COULD_NOT_OPEN_BODY =
        "UNFYND could not render that PDF page. Try again, or reconnect the folder."

    const val DISMISS_OPEN_FEEDBACK_LABEL = "Dismiss"

    const val PREVIEW_TITLE = "Original PDF page"

    const val CLOSE_PREVIEW_LABEL = "Back to results"

    const val PREVIEW_SCOPE_BODY =
        "This is a read-only preview of the page UNFYND cited. " +
            "Search still used saved text on this phone — not a live re-read of the whole file."

    fun previewPageCaption(pageNumber: Int, pageCount: Int): String {
        require(pageNumber > 0 && pageCount > 0 && pageNumber <= pageCount)
        return "Page $pageNumber of $pageCount"
    }

    /**
     * TalkBack description for the read-only cited-page image.
     * Names the document and page; does not claim OCR or meaning.
     */
    fun previewImageContentDescription(
        documentLabel: String,
        pageNumber: Int,
        pageCount: Int,
    ): String {
        require(documentLabel.isNotBlank()) { "Preview image description needs a document label." }
        return "Read-only preview of \"$documentLabel\", ${previewPageCaption(pageNumber, pageCount)}."
    }
}
