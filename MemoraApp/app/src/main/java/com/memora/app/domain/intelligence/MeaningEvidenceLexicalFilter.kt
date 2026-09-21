package com.memora.app.domain.intelligence

/**
 * Lexical AND precision gate for meaning recall (F-02 + scenario bar MF-1).
 *
 * When the cue names one or more content tokens, [satisfies] is the exact AND
 * over those tokens. [PreparedCue.matchingTokens] is the same compilation
 * counted per word, which [MeaningRoleScorer] and the precision tier
 * (D-12) both read. Semantic cosine remains candidate generation. Not keyword
 * Find and not measured AVAILABLE.
 *
 * The token list comes from [MeaningRecallCue.contentTokens] so that precision
 * and embed text always agree on what the person named (defect D-10). Pool
 * admission and page order read [MeaningRoleScorer] (ADR-055).
 */
object MeaningEvidenceLexicalFilter {
    /**
     * Precision over an explicit token list, so a caller can drop the words a
     * structured constraint already consumed. `notes in 2024` must still require
     * `notes` in stored text but never the literal `2024`, which lives on a TIME
     * anchor instead (bar T10).
     */
    fun satisfies(tokens: List<String>, evidenceText: String): Boolean =
        prepare(tokens).satisfies(evidenceText)

    /**
     * Compile a cue once when it will be checked against many candidates.
     *
     * [EnglishRecallInflection.occursAsWholeWord] builds a fresh [Regex] for
     * every variant on every call. That is affordable for one ranked hit and is
     * not affordable across a whole corpus, which is what candidate admission
     * now asks of this gate (defect D-11).
     */
    fun prepare(tokens: List<String>): PreparedCue =
        PreparedCue(
            tokens.map { token ->
                TokenCue(
                    token = token,
                    variants = EnglishRecallInflection.wholeWordVariants(token).map { variant ->
                        Regex("""\b${Regex.escape(variant)}\b""")
                    },
                )
            },
        )

    /**
     * One cue compiled for repeated evaluation. A token with no usable variant
     * can never be satisfied, matching [EnglishRecallInflection.occursAsWholeWord].
     *
     * [satisfies] is the exact AND. [matchingTokens] is the same compilation
     * counted per word, so [MeaningRoleScorer] and the precision tier (D-12)
     * cannot disagree about which named words a Memory carries.
     */
    class PreparedCue internal constructor(
        private val tokens: List<TokenCue>,
    ) {
        val isEmpty: Boolean get() = tokens.isEmpty()

        fun satisfies(evidenceText: String): Boolean {
            if (tokens.isEmpty()) return true
            val haystack = evidenceText.lowercase()
            return tokens.all { it.matches(haystack) }
        }

        /**
         * Named words from this cue that appear in [evidenceText], in cue order.
         * Empty cue → empty list (there is nothing to match), which is why
         * [satisfies] and [matchingTokens] diverge on the empty case.
         */
        fun matchingTokens(evidenceText: String): List<String> {
            if (tokens.isEmpty()) return emptyList()
            val haystack = evidenceText.lowercase()
            return tokens.filter { it.matches(haystack) }.map { it.token }
        }
    }

    internal data class TokenCue(
        val token: String,
        val variants: List<Regex>,
    ) {
        fun matches(haystack: String): Boolean =
            variants.any { it.containsMatchIn(haystack) }
    }
}
