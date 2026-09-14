package com.memora.app.ui.search

/**
 * One Why dialect for every Canonical Recall hit
 * (`docs/CANONICAL_RECALL_RESULT_CONTRACT.md` §3.4 "one dialect").
 *
 * Same slots, same order, same emphasis on PDF / photo / screenshot / note and
 * on both retrieval paths:
 *
 * 1. relevance — [askPrefix] + [ask] then [subjectPrefix] + [subject]
 * 2. [citedLine] — the stored line that justifies the hit
 * 3. [howFound] — which retrieval path found it (contract §3.2, ADR-024)
 *
 * [citedLine] is the one conditional slot, and the rule is explicit: Why quotes
 * stored text only when the result card does not already show it. Keyword cards
 * render a highlighted excerpt, so repeating it inside Why is the U5
 * "duplicate full card excerpt" anti-pattern that defect D-17 removed from the
 * meaning path — the keyword path kept it until this slice.
 *
 * Why never states the page or the file name: both are already on the card.
 * Missing-word honesty for a partial meaning list stays on the result-list
 * banner (D-12), never on a card, so one banner can describe the whole list.
 */
data class WhyPresentation(
    val askPrefix: String,
    val ask: String,
    val subjectPrefix: String,
    val subject: String,
    val citedLine: String?,
    val howFound: String,
) {
    init {
        require(askPrefix.isNotBlank()) { "Why needs a lead-in for what was asked." }
        require(ask.isNotBlank()) { "Why needs the words the person asked for." }
        require(subjectPrefix.isNotBlank()) { "Why needs a lead-in for this file." }
        require(subject.isNotBlank()) { "Why needs what this file is or carries." }
        require(citedLine == null || citedLine.isNotBlank()) {
            "A cited line must be stored text, not an empty quote."
        }
        require(howFound.isNotBlank()) { "Why must say how this file was found." }
    }

    /** The relevance sentence pair, unstyled. */
    val relevanceText: String
        get() = "$askPrefix$ask. $subjectPrefix$subject."

    /**
     * Screen-reader description and the assertion surface for unit tests: the
     * whole panel as one calm sentence sequence.
     */
    val spokenText: String
        get() = buildList {
            add(relevanceText)
            citedLine?.let { add("\"$it\"") }
            add(howFound)
        }.joinToString(" ")
}
