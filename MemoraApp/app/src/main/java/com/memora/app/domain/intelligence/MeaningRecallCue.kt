package com.memora.app.domain.intelligence

/**
 * Product recall cue for Find by meaning (scenario bar MF-1).
 *
 * Natural-language questions are first-class: we normalize the raw string, then
 * derive content tokens for embedding and precision. Keyword Find may keep its
 * own normalize helper; meaning must not embed filler-only questions as-is.
 */
object MeaningRecallCue {
    const val MAX_QUERY_CHARS = 120

    fun normalize(rawQuery: String): String =
        rawQuery.trim().replace(WHITESPACE, " ").take(MAX_QUERY_CHARS)

    fun contentTokens(rawQuery: String): List<String> =
        RecallQueryContentTokens.tokens(normalize(rawQuery))

    /**
     * Text passed to the embedding engine for candidate generation.
     * When content tokens exist (e.g. silky from "which file has silky in it"),
     * embed those tokens so NL wrappers do not dilute the vector.
     */
    fun embedText(rawQuery: String): String {
        val normalized = normalize(rawQuery)
        if (normalized.isEmpty()) return normalized
        val tokens = RecallQueryContentTokens.tokens(normalized)
        return if (tokens.isNotEmpty()) tokens.joinToString(" ") else normalized
    }

    /** Query string shown in Why / empty states. */
    fun displayQuery(rawQuery: String): String = normalize(rawQuery)

    private val WHITESPACE = Regex("""\s+""")
}
