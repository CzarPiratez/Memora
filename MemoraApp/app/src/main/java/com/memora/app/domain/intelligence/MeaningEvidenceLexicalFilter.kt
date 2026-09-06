package com.memora.app.domain.intelligence

/**
 * Lexical AND precision gate for meaning recall (F-02 + scenario bar MF-1).
 *
 * When the cue names one or more content tokens, every token must appear in the
 * ranked evidence text. Semantic cosine remains candidate generation; this filter
 * removes soft neighbors that do not support the named cue. Not keyword Find and
 * not measured AVAILABLE.
 *
 * The token list comes from [MeaningRecallCue.contentTokens] so that precision
 * and candidate generation always agree on what the person named (defect D-10).
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
                EnglishRecallInflection.wholeWordVariants(token).map { variant ->
                    Regex("""\b${Regex.escape(variant)}\b""")
                }
            },
        )

    /**
     * One cue compiled for repeated evaluation. A token with no usable variant
     * can never be satisfied, matching [EnglishRecallInflection.occursAsWholeWord].
     */
    class PreparedCue internal constructor(
        private val variantsPerToken: List<List<Regex>>,
    ) {
        val isEmpty: Boolean get() = variantsPerToken.isEmpty()

        fun satisfies(evidenceText: String): Boolean {
            if (variantsPerToken.isEmpty()) return true
            val haystack = evidenceText.lowercase()
            return variantsPerToken.all { variants ->
                variants.any { it.containsMatchIn(haystack) }
            }
        }
    }
}
