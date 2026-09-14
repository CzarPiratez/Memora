package com.memora.app.ui.search

import com.memora.app.application.images.PhotoOcrKeywordSearchHit
import com.memora.app.application.memory.CanonicalRecallResult

/**
 * Plain-language copy for interim keyword search over photo Memory evidence.
 *
 * Must not claim meaning-based Memory recall, AI understanding, screenshot OCR,
 * or cloud.
 */
object PhotoOcrKeywordSearchCopy {
    const val SCREEN_TITLE = "Find saved photo text"

    const val SCOPE_BODY =
        "Search matches exact words in photo Memory evidence saved on this phone. " +
            "Your original photos stay where they are. Screenshots are not included."

    const val QUERY_LABEL = "Words to find"
    const val SEARCH_LABEL = "Search on this phone"
    const val CLEAR_QUERY_LABEL = "Clear"
    const val CANCEL_SEARCH_LABEL = "Cancel search"
    const val EMPTY_QUERY_BODY = "Type a word or short phrase, then search."
    const val SEARCHING_BODY = "Searching saved photo memory evidence on this phone…"
    const val NOTHING_SAVED_BODY =
        "Nothing is ready for photo keyword search yet. Finish Read text from photos " +
            "and Memory assembly in photo setup first."
    const val SEARCH_COULD_NOT_FINISH_BODY = "Search could not finish. Try again in a moment."
    const val WHY_THIS_RESULT_LABEL = CanonicalRecallWhyCopy.WHY_THIS_RESULT_LABEL
    const val HIDE_WHY_LABEL = CanonicalRecallWhyCopy.HIDE_WHY_LABEL
    const val OPEN_ORIGINAL_LABEL = "Open original"
    const val OPENING_BODY = "Opening a read-only preview of that photo…"
    const val SOURCE_UNAVAILABLE_BODY =
        "UNFYND cannot open that photo right now. Photo access may have changed."
    const val COULD_NOT_OPEN_BODY = "UNFYND could not show that photo."
    const val DISMISS_LABEL = "Dismiss"
    const val BACK_LABEL = "Back"
    const val PREVIEW_TITLE = "Original photo"
    const val CLOSE_PREVIEW_LABEL = "Back to results"
    const val PREVIEW_SCOPE_BODY =
        "This is a read-only preview of the photo UNFYND cited. " +
            "Search still used saved Memory evidence on this phone — not a live re-read " +
            "of the image for keywords."

    fun previewImageContentDescription(photoLabel: String): String {
        require(photoLabel.isNotBlank()) { "Preview image description needs a photo label." }
        return "Read-only preview of \"$photoLabel\"."
    }

    fun noMatchesBody(query: String) =
        "No photo memory evidence on this phone matched \"$query\"."

    /**
     * Honest inventory of READY photo Memory assets for keyword search.
     *
     * Counts distinct photos with READY Memory evidence (not raw OCR
     * extraction DAO rows) after MIG-07.
     */
    fun readinessBody(photoCount: Int): String {
        require(photoCount >= 0) { "Readiness photo count cannot be negative." }
        if (photoCount == 0) {
            return "Nothing is ready for photo keyword search yet. " +
                "Finish Read text from photos and Memory assembly in photo setup first."
        }
        val items = if (photoCount == 1) "1 photo" else "$photoCount photos"
        val verb = if (photoCount == 1) "is" else "are"
        return "$items with READY Memory evidence $verb ready for exact-word search on this phone."
    }

    fun resultsSummary(query: String, count: Int, capped: Boolean): String =
        "Results for \"$query\". Showing $count ${if (count == 1) "match" else "matches"}." +
            if (capped) " UNFYND lists at most 20 matches." else ""

    fun whyThisResultBody(query: String, recall: CanonicalRecallResult): String =
        CanonicalRecallWhyCopy.whyThisResult(recall, query)

    fun whyThisResultBody(hit: PhotoOcrKeywordSearchHit, query: String): String =
        whyThisResultBody(query, hit.recall)
}
