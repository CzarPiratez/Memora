package com.memora.app.domain.intelligence

/**
 * Lexical AND precision gate for meaning recall (F-02 + scenario bar MF-1).
 *
 * When the cue names one or more content tokens, every token must appear in the
 * ranked evidence text. Semantic cosine remains candidate generation; this filter
 * removes soft neighbors that do not support the named cue. Not keyword Find and
 * not measured AVAILABLE.
 */
object MeaningEvidenceLexicalFilter {
    fun requiredContentTokens(query: String): List<String> =
        RecallQueryContentTokens.tokens(query)

    /** Precision gate when the cue names at least one content token (bar T1 / U1). */
    fun shouldApply(query: String): Boolean = requiredContentTokens(query).isNotEmpty()

    fun evidenceSatisfies(query: String, evidenceText: String): Boolean {
        val tokens = requiredContentTokens(query)
        if (tokens.isEmpty()) return true
        val haystack = evidenceText.lowercase()
        return tokens.all { token ->
            EnglishRecallInflection.occursAsWholeWord(haystack, token)
        }
    }
}
