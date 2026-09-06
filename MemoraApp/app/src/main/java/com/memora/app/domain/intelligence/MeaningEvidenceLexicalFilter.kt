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
    fun satisfies(tokens: List<String>, evidenceText: String): Boolean {
        if (tokens.isEmpty()) return true
        val haystack = evidenceText.lowercase()
        return tokens.all { token ->
            EnglishRecallInflection.occursAsWholeWord(haystack, token)
        }
    }
}
