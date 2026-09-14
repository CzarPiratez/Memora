package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.memory.CanonicalRecallResult
import com.memora.app.application.memory.CanonicalRecallRetrievalPath
import com.memora.app.domain.intelligence.RecallQueryContentTokens

/**
 * Single Why assembler for every Canonical Recall hit (contract §3).
 *
 * One dialect, one shape, one visual treatment across PDF, photo, screenshot,
 * and note, on both retrieval paths. Per-screen copy objects must not add a
 * Why string of their own — four asset Explain dialects is the state the
 * shared-result contract exists to prevent.
 *
 * Why answers three things and nothing else:
 *
 * - **Relevance** — what you asked, paired with what this file is or carries.
 * - **Evidence** — the stored line that justifies it, quoted, *only when the
 *   card does not already show it* (U5; defect D-17).
 * - **Provenance** — which path found it (contract §3.2), in consumer words.
 *
 * It never repeats the file name, the page, or the excerpt the card is already
 * showing, never claims a synonym, and never invents a fact.
 */
object CanonicalRecallWhyCopy {
    const val WHY_THIS_RESULT_LABEL = "Why this result?"

    const val HIDE_WHY_LABEL = "Hide explanation"

    /**
     * Provenance in consumer language. Keyword must never read as meaning
     * (ADR-024); meaning must name meaning rather than leaving the path to the
     * screen title, and must not leak similarity or score jargon (device
     * runbook F-03).
     */
    const val FOUND_BY_KEYWORD = "Found by exact words in evidence saved on this phone."

    const val FOUND_BY_MEANING = "Found by meaning, from memories saved on this phone."

    /**
     * Token boost is a disclosed hybrid assist
     * ([com.memora.app.domain.intelligence.MeaningEvidenceTokenBoost]). It says
     * *a* word helped — never that every word the person used was present,
     * which is the claim defect D-17 removed and which would contradict a
     * partial-match banner.
     */
    const val FOUND_BY_MEANING_WITH_WORD_ASSIST =
        "Found by meaning, helped by a word you typed, from memories saved on this phone."

    /** Ranked meaning hit: cited line reads the whole Memory, not one snippet. */
    fun present(hit: MeaningSearchHit, query: String): WhyPresentation {
        require(query.isNotBlank()) { "Why needs the submitted query." }
        return meaningPresentation(
            explanation = MeaningWhy.explain(hit, query),
            evidenceTokenBoosted = hit.evidenceTokenBoosted,
        )
    }

    fun present(result: CanonicalRecallResult, query: String): WhyPresentation {
        require(query.isNotBlank()) { "Why needs the submitted query." }
        return when (result.retrievalPath) {
            CanonicalRecallRetrievalPath.KEYWORD -> keywordPresentation(result, query)
            CanonicalRecallRetrievalPath.MEANING -> meaningPresentation(
                explanation = MeaningWhy.explain(
                    label = result.label,
                    haystack = result.excerpt,
                    summaryText = result.excerpt,
                    query = query,
                ),
                evidenceTokenBoosted = result.evidenceTokenBoosted,
            )
        }
    }

    /** Spoken form: screen-reader description and unit-test assertion surface. */
    fun whyThisResult(result: CanonicalRecallResult, query: String): String =
        present(result, query).spokenText

    fun whyThisResult(hit: MeaningSearchHit, query: String): String =
        present(hit, query).spokenText

    private fun meaningPresentation(
        explanation: MeaningWhy.Explanation,
        evidenceTokenBoosted: Boolean,
    ): WhyPresentation = WhyPresentation(
        askPrefix = ASK_PREFIX,
        ask = explanation.asked,
        subjectPrefix = "This file is ",
        subject = explanation.fileIs,
        citedLine = explanation.citedLine,
        howFound = if (evidenceTokenBoosted) {
            FOUND_BY_MEANING_WITH_WORD_ASSIST
        } else {
            FOUND_BY_MEANING
        },
    )

    /**
     * Keyword cards already render the matching excerpt with the match
     * emphasised, so Why states where those words are instead of printing the
     * same span a second time.
     */
    private fun keywordPresentation(
        result: CanonicalRecallResult,
        query: String,
    ): WhyPresentation {
        require(result.retrievalPath == CanonicalRecallRetrievalPath.KEYWORD) {
            "Keyword Why cannot present a meaning hit."
        }
        val typed = normalizeForDisplay(query)
        return WhyPresentation(
            askPrefix = ASK_PREFIX,
            ask = "\"$typed\"",
            subjectPrefix = when (RecallQueryContentTokens.tokens(typed).size) {
                0 -> "That text is "
                1 -> "That word is "
                else -> "Those words are "
            },
            subject = "in this file's saved text",
            citedLine = null,
            howFound = FOUND_BY_KEYWORD,
        )
    }

    private fun normalizeForDisplay(query: String): String =
        query.trim().replace(WHITESPACE, " ").take(MAX_ASK_CHARS)

    internal fun friendlyDisplayLabel(label: String): String {
        val separator = label.indexOf('_')
        if (separator in 33..45 && label.substring(0, separator).all { it.isDigit() || it in 'a'..'f' }) {
            return label.substring(separator + 1)
        }
        return label
    }

    private const val ASK_PREFIX = "You asked about "

    private const val MAX_ASK_CHARS = 120

    private val WHITESPACE = Regex("""\s+""")
}
