package com.memora.app.ui.search

import com.memora.app.application.memory.CanonicalRecallResult

/**
 * Plain-language copy for interim keyword search over screenshot Memory evidence.
 *
 * Must not claim meaning-based Memory recall, AI understanding, PHOTO OCR, or cloud.
 * Why lives in [CanonicalRecallWhyCopy] — one dialect for every hit.
 */
object ScreenshotOcrKeywordSearchCopy {
    const val SCREEN_TITLE = "Find saved screenshot text"

    const val SCOPE_BODY =
        "Search matches exact words in screenshot Memory evidence saved on this phone. " +
            "Your original screenshots stay where they are. Ordinary photos are not included."

    const val QUERY_LABEL = "Words to find"

    const val SEARCH_LABEL = "Search on this phone"

    const val CLEAR_QUERY_LABEL = "Clear"

    const val CANCEL_SEARCH_LABEL = "Cancel search"

    const val EMPTY_QUERY_BODY = "Type a word or short phrase, then search."

    const val SEARCHING_BODY =
        "Searching saved screenshot memory evidence on this phone…"

    fun noMatchesBody(query: String): String {
        require(query.isNotBlank()) { "No-matches copy needs the submitted query." }
        return "No screenshot memory evidence on this phone matched \"$query\". " +
            "Try different words, or finish Read text from screenshots and Memory assembly first."
    }

    const val NOTHING_SAVED_BODY =
        "Nothing is ready for screenshot keyword search yet. Finish Read text from " +
            "screenshots and Memory assembly in photo setup first."

    const val READINESS_LOADING_BODY = "Checking screenshot memory evidence on this phone…"

    const val READINESS_COULD_NOT_LOAD_BODY =
        "Could not check how much screenshot memory evidence is ready for search on this phone. " +
            "Try opening this screen again in a moment."

    /**
     * Honest inventory of READY screenshot Memory assets for keyword search.
     *
     * Counts distinct screenshots with READY Memory evidence (not raw OCR
     * extraction DAO rows) after MIG-07.
     */
    fun readinessBody(screenshotCount: Int): String {
        require(screenshotCount >= 0) { "Readiness screenshot count cannot be negative." }
        if (screenshotCount == 0) {
            return "Nothing is ready for screenshot keyword search yet. " +
                "Finish Read text from screenshots and Memory assembly in photo setup first."
        }
        val items = if (screenshotCount == 1) {
            "1 screenshot"
        } else {
            "$screenshotCount screenshots"
        }
        val verb = if (screenshotCount == 1) "is" else "are"
        return "$items with READY Memory evidence $verb ready for exact-word search on this phone."
    }

    const val SEARCH_COULD_NOT_FINISH_BODY =
        "Search could not finish on this phone. Try again in a moment. " +
            "UNFYND does not reopen your original screenshots for this search."

    const val RESULTS_HINT =
        "Matches show the screenshot name and a short excerpt from saved Memory evidence. " +
            "Open Why this result? to see why this file matched. " +
            "Open original shows a read-only preview inside UNFYND."

    const val MAX_LISTED_MATCHES = 20

    const val OPEN_ORIGINAL_SCREENSHOT_LABEL = "Open original"

    const val OPEN_ORIGINAL_SCREENSHOT_HINT =
        "Opens a read-only preview of this screenshot inside UNFYND. " +
            "Your file stays where it is; UNFYND does not edit it. " +
            "Search still used saved Memory evidence on this phone."

    const val OPEN_FEEDBACK_OPENING_BODY =
        "Opening a read-only preview of that screenshot…"

    const val OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY =
        "UNFYND cannot open that screenshot right now. The saved link may be stale " +
            "after a phone restart, or photo access may have been removed. " +
            "Try Start indexing again in photo setup, then Open original once more."

    const val OPEN_FEEDBACK_COULD_NOT_OPEN_BODY =
        "UNFYND could not show that screenshot. Try again in a moment."

    const val DISMISS_OPEN_FEEDBACK_LABEL = "Dismiss"

    const val PREVIEW_TITLE = "Original screenshot"

    const val CLOSE_PREVIEW_LABEL = "Back to results"

    const val PREVIEW_SCOPE_BODY =
        "This is a read-only preview of the screenshot UNFYND cited. " +
            "Search still used saved Memory evidence on this phone — not a live re-read " +
            "of the image for keywords."

    fun previewImageContentDescription(screenshotLabel: String): String {
        require(screenshotLabel.isNotBlank()) {
            "Preview image description needs a screenshot label."
        }
        return "Read-only preview of \"$screenshotLabel\"."
    }

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
                "more saved screenshots may also contain these words."
        } else {
            ""
        }
        return forLine + countLine + capLine + " " + RESULTS_HINT
    }

    const val BACK_LABEL = "Back"

    const val WHY_THIS_RESULT_LABEL = CanonicalRecallWhyCopy.WHY_THIS_RESULT_LABEL

    fun whyThisResultBody(query: String, recall: CanonicalRecallResult): String =
        CanonicalRecallWhyCopy.whyThisResult(recall, query)
}
