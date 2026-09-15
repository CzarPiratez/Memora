package com.memora.app.ui.search

import com.memora.app.application.memory.CanonicalRecallResult
import com.memora.app.application.notes.NotePageKeywordSearchHit

/**
 * Plain-language copy for keyword search over note Memory evidence.
 *
 * Must not claim meaning-based Memory recall, AI understanding, or cloud search
 * during the keyword Find itself. Open-original may need network/OneNote.
 */
object NotePageKeywordSearchCopy {
    const val SCREEN_TITLE = "Find saved note text"

    const val ENTRY_LABEL = "Find saved note text"

    const val SCOPE_BODY =
        "Search matches exact words in note Memory evidence saved on this phone. " +
            "Search uses saved evidence on this phone."

    const val QUERY_LABEL = "Words to find"

    const val SEARCH_LABEL = "Search on this phone"

    const val CLEAR_QUERY_LABEL = "Clear"

    const val CANCEL_SEARCH_LABEL = "Cancel search"

    const val EMPTY_QUERY_BODY = "Type a word or short phrase, then search."

    const val SEARCHING_BODY =
        "Searching saved note memory evidence on this phone…"

    fun noMatchesBody(query: String): String {
        require(query.isNotBlank()) { "No-matches copy needs the submitted query." }
        return "No note memory evidence on this phone matched \"$query\". " +
            "Try different words, or finish Extract OneNote page text and Memory " +
            "assembly in Notes indexing first."
    }

    const val NOTHING_SAVED_BODY =
        "Nothing is ready for note keyword search yet. In Notes indexing, Connect OneNote, " +
            "Discover pages, Extract OneNote page text, then finish Memory assembly."

    const val READINESS_LOADING_BODY = "Checking saved note memory evidence on this phone…"

    const val READINESS_COULD_NOT_LOAD_BODY =
        "Could not check how much note memory evidence is ready for search on this phone. " +
            "Try opening this screen again in a moment."

    /**
     * Honest inventory of READY note Memory assets for keyword search.
     *
     * Counts distinct notes with READY Memory evidence (not raw note
     * extraction DAO rows) after MIG-07.
     */
    fun readinessBody(noteCount: Int): String {
        require(noteCount >= 0) { "Readiness note count cannot be negative." }
        if (noteCount == 0) {
            return "Nothing is ready for note keyword search yet. " +
                "In Notes indexing, Connect, Discover, Extract OneNote page text, " +
                "then finish Memory assembly first."
        }
        val items = if (noteCount == 1) "1 note" else "$noteCount notes"
        val verb = if (noteCount == 1) "is" else "are"
        return "$items with READY Memory evidence $verb ready for exact-word search on this phone."
    }

    const val SEARCH_COULD_NOT_FINISH_BODY =
        "Search could not finish on this phone. Try again in a moment. " +
            "UNFYND does not call Microsoft for this search."

    const val RESULTS_HINT =
        "Matches show the page title and a short excerpt from saved note Memory evidence. " +
            "Open Why this result? to see why this file matched."

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
            " UNFYND lists at most $MAX_LISTED_MATCHES matches for now; " +
                "more saved notes may also contain these words."
        } else {
            ""
        }
        return forLine + countLine + capLine + " " + RESULTS_HINT
    }

    const val WHY_THIS_RESULT_LABEL = CanonicalRecallWhyCopy.WHY_THIS_RESULT_LABEL

    const val HIDE_WHY_LABEL = CanonicalRecallWhyCopy.HIDE_WHY_LABEL

    const val OPEN_ORIGINAL_NOTE_LABEL = "Open original note"

    const val OPEN_ORIGINAL_NOTE_HINT =
        "Opens this page in the OneNote app when it is installed on this phone; " +
            "otherwise it opens in your browser. That step may need a network connection " +
            "and your Microsoft OneNote connection. UNFYND does not edit the original. " +
            "Keyword search still used saved Memory evidence on this phone."

    const val OPEN_FEEDBACK_OPENING_BODY =
        "Opening that page in OneNote or your browser…"

    /** Sits inside the button, so it has to be short. */
    const val OPENING_LABEL = "Opening…"

    const val OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY =
        "UNFYND could not open that original note because the Microsoft OneNote connection " +
            "needs to be renewed. Open Notes indexing, tap Connect OneNote, finish sign-in " +
            "inside UNFYND, then try Open original note again. " +
            "Keyword search still uses Memory evidence saved on this phone."

    const val OPEN_FEEDBACK_COULD_NOT_OPEN_BODY =
        "UNFYND could not open that page in OneNote or a browser. Check your network and try again. " +
            "Keyword search still uses Memory evidence saved on this phone."

    const val DISMISS_OPEN_FEEDBACK_LABEL = "Dismiss"

    /** Everything one result card needs to run and report its own open. */
    val OPEN_ORIGINAL = FindOpenOriginalCopy(
        openLabel = OPEN_ORIGINAL_NOTE_LABEL,
        openingLabel = OPENING_LABEL,
        openingAnnouncement = OPEN_FEEDBACK_OPENING_BODY,
        sourceUnavailableBody = OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY,
        couldNotOpenBody = OPEN_FEEDBACK_COULD_NOT_OPEN_BODY,
        dismissLabel = DISMISS_OPEN_FEEDBACK_LABEL,
    )

    const val BACK_LABEL = "Back"

    fun whyThisResultBody(query: String, recall: CanonicalRecallResult): String =
        CanonicalRecallWhyCopy.whyThisResult(recall, query)

    fun whyThisResultBody(hit: NotePageKeywordSearchHit, query: String): String =
        whyThisResultBody(query, hit.recall)
}
