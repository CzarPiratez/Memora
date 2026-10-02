package com.memora.app.domain.intelligence

import com.memora.app.domain.memory.RecallQueryConstraintClassifier

/**
 * Product recall cue for Find by meaning (scenario bar MF-1).
 *
 * Natural-language questions are first-class: we normalize the raw string, then
 * derive content tokens for embedding and precision (I1 wrappers + content).
 * Keyword Find may keep its own normalize helper; meaning must not embed
 * filler-only questions as-is.
 */
object MeaningRecallCue {
    const val MAX_QUERY_CHARS = 120
    const val MIN_CANDIDATE_SCORE = 0.05f

    fun normalize(rawQuery: String): String =
        rawQuery.trim().replace(WHITESPACE, " ").take(MAX_QUERY_CHARS)

    /**
     * What the person named as content: the words left after ask-shape wrappers
     * and after any word a structured constraint already owns.
     *
     * Candidate generation and the lexical precision gate must both read this
     * one derivation. While they derived it separately they disagreed, and
     * `recent files with silky` embedded `recent silky` while requiring only
     * `silky` — the vector drifted toward recency language, the single file
     * containing `silky` fell out of the candidate pool, and the query answered
     * nothing while bare `silky` worked (bar T10, defect D-10).
     */
    fun contentTokens(rawQuery: String): List<String> {
        val normalized = normalize(rawQuery)
        val named = RecallQueryContentTokens.tokens(normalized)
        if (named.isEmpty()) return named
        val consumed = constraintConsumedTokens(normalized)
        return if (consumed.isEmpty()) named else named - consumed
    }

    /**
     * Text passed to the embedding engine for candidate generation.
     * When content tokens exist (e.g. silky from "which file has silky in it"),
     * embed those tokens so NL wrappers do not dilute the vector.
     */
    fun embedText(rawQuery: String): String {
        val normalized = normalize(rawQuery)
        if (normalized.isEmpty()) return normalized
        val tokens = contentTokens(normalized)
        return if (tokens.isNotEmpty()) tokens.joinToString(" ") else normalized
    }

    /** Query string shown in Why / empty states. */
    fun displayQuery(rawQuery: String): String = normalize(rawQuery)

    /** Ask-shape wrappers were present (`when are…`, `show me…`). */
    fun hadAskShape(rawQuery: String): Boolean =
        RecallQueryContentTokens.hadAskShape(rawQuery)

    /**
     * Words a TIME expression owns (`recent`, `last week`, `in 2024`). They say
     * which Memories qualify; they are not text to retrieve on, so neither the
     * query vector nor the precision gate may treat them as named content.
     */
    private fun constraintConsumedTokens(normalizedQuery: String): Set<String> =
        RecallQueryConstraintClassifier.classify(normalizedQuery)
            .timeSpanText
            ?.let { RecallQueryContentTokens.namedTokens(it).toSet() }
            .orEmpty()

    private val WHITESPACE = Regex("""\s+""")
}
