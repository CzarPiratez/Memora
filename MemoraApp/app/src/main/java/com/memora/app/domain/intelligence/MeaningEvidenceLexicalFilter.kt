package com.memora.app.domain.intelligence

/**
 * Lexical AND constraint for meaning recall when a cue carries multiple content
 * tokens (A-01 follow-up F-02).
 *
 * Semantic cosine ranking remains candidate generation; this filter excludes hits
 * whose ranked evidence text does not contain every required cue token. Not
 * keyword Find and not measured AVAILABLE.
 */
object MeaningEvidenceLexicalFilter {
    fun requiredContentTokens(query: String): List<String> =
        RecallQueryContentTokens.tokens(query)

    fun shouldApply(query: String): Boolean = requiredContentTokens(query).size >= 2

    fun evidenceSatisfies(query: String, evidenceText: String): Boolean {
        val tokens = requiredContentTokens(query)
        if (tokens.size < 2) return true
        val haystack = evidenceText.lowercase()
        return tokens.all { token ->
            Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(haystack)
        }
    }
}
