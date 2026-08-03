package com.memora.app.application.intelligence

/**
 * Disclosed hybrid assist for candidate meaning ranking (compact embedder gap).
 *
 * When a significant cue token appears in the ranked evidence text, cosine
 * score receives a bounded boost. This is not keyword Find, not measured
 * AVAILABLE, and must be disclosed in Why copy when applied.
 */
object MeaningEvidenceTokenBoost {
    const val TOKEN_BOOST = 0.35f
    const val MIN_TOKEN_CHARS = 3

    fun significantTokens(query: String): List<String> =
        TOKEN_SPLIT.split(query.lowercase())
            .map { it.trim() }
            .filter { it.length >= MIN_TOKEN_CHARS }

    fun evidenceContainsCueToken(query: String, evidenceText: String): Boolean {
        val tokens = significantTokens(query)
        if (tokens.isEmpty()) return false
        val haystack = evidenceText.lowercase()
        return tokens.any { token ->
            Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(haystack)
        }
    }

    /**
     * @return Pair of final score (0..1) and whether the token boost applied.
     */
    fun apply(cosine: Float, query: String, evidenceText: String): Pair<Float, Boolean> {
        require(cosine.isFinite())
        val boosted = evidenceContainsCueToken(query, evidenceText)
        if (!boosted) return cosine.coerceIn(0f, 1f) to false
        return (cosine + TOKEN_BOOST).coerceIn(0f, 1f) to true
    }

    private val TOKEN_SPLIT = Regex("""[^a-z0-9]+""")
}
