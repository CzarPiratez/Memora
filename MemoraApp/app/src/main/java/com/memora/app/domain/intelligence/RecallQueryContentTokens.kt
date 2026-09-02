package com.memora.app.domain.intelligence

/**
 * Content tokens parsed from a recall cue (keyword or meaning lexical filter).
 *
 * Splits on punctuation and drops connector/stop words so `silky, wreck` and
 * `scan and silky` behave like `silky wreck` / `scan silky`.
 */
object RecallQueryContentTokens {
    const val MIN_TOKEN_LENGTH = 2

    private val TOKEN_SPLIT = Regex("""[^a-z0-9]+""")

    private val STOP_WORDS = setOf(
        "a",
        "an",
        "and",
        "are",
        "but",
        "can",
        "do",
        "does",
        "file",
        "files",
        "find",
        "for",
        "from",
        "has",
        "have",
        "how",
        "in",
        "is",
        "it",
        "not",
        "of",
        "on",
        "or",
        "that",
        "the",
        "this",
        "to",
        "what",
        "when",
        "where",
        "which",
        "who",
        "why",
        "with",
        "your",
    )

    fun tokens(rawQuery: String): List<String> =
        TOKEN_SPLIT.split(rawQuery.lowercase())
            .map { it.trim() }
            .filter { it.length >= MIN_TOKEN_LENGTH && it !in STOP_WORDS }
}
