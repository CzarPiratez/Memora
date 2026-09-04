package com.memora.app.domain.intelligence

/**
 * Content tokens parsed from a recall cue (keyword or meaning lexical filter).
 *
 * Ask-shape is a **closed English catalog** (verbs, type nouns, discourse,
 * abbreviations, spoken quantities) — not a screenshot-driven allowlist. Domain
 * words (`scan`, `silky`, `training`, `list`) stay content. Not general NLU. Not I3 list-split.
 * Not a synonym net (`silky` ≠ `smooth`).
 */
object RecallQueryContentTokens {
    const val MIN_TOKEN_LENGTH = 2

    private val TOKEN_SPLIT = Regex("""[^a-z0-9]+""")

    /** `e.g.s` / `e.g.` / `eg.` → tokens that the wrapper catalog can drop. */
    private val EG_ABBREV = Regex("""\be\.?\s*g\.?s?\b""", RegexOption.IGNORE_CASE)

    private val ASK_VERBS = setOf(
        "bring",
        "display",
        "fetch",
        "find",
        "get",
        "give",
        "grab",
        "help",
        "locate",
        "look",
        "looking",
        "need",
        "needed",
        "needs",
        "open",
        "please",
        "pls",
        "plz",
        "pull",
        "retrieve",
        "search",
        "searching",
        "see",
        "show",
        "want",
        "wanted",
        "wanting",
    )

    private val ASK_TYPE_NOUNS = setOf(
        "attachment",
        "attachments",
        "doc",
        "docs",
        "document",
        "documents",
        "file",
        "files",
        "image",
        "images",
        "pdf",
        "pdfs",
        "photo",
        "photos",
        "picture",
        "pictures",
        "screenshot",
        "screenshots",
    )

    /**
     * How many files the person wants — not a word in the PDF.
     * Spoken 1–12 only; digits of length 1 are already dropped by [MIN_TOKEN_LENGTH].
     * Does not treat `year 4` as quantity (`4` is a digit). `two` in "give me two
     * files" is quantity; rare "times table two" may over-strip (P0 tradeoff).
     */
    private val RESULT_QUANTITY = setOf(
        "both",
        "dozen",
        "eight",
        "eleven",
        "five",
        "four",
        "half",
        "lots",
        "many",
        "nine",
        "one",
        "pair",
        "seven",
        "single",
        "six",
        "ten",
        "three",
        "twelve",
        "two",
    )

    private val FUNCTION_WORDS = setOf(
        "a",
        "all",
        "am",
        "an",
        "and",
        "any",
        "are",
        "as",
        "at",
        "be",
        "been",
        "being",
        "but",
        "by",
        "can",
        "could",
        "couple",
        "do",
        "does",
        "eg",
        "egs",
        "etc",
        "every",
        "few",
        "for",
        "gonna",
        "got",
        "gotta",
        "has",
        "have",
        "hey",
        "hi",
        "how",
        "i",
        "ie",
        "if",
        "in",
        "into",
        "is",
        "it",
        "its",
        "just",
        "kinda",
        "maybe",
        "me",
        "might",
        "more",
        "my",
        "not",
        "of",
        "on",
        "ones",
        "onto",
        "or",
        "our",
        "out",
        "per",
        "really",
        "several",
        "should",
        "so",
        "some",
        "something",
        "sorta",
        "still",
        "stuff",
        "than",
        "thanks",
        "that",
        "the",
        "their",
        "them",
        "then",
        "there",
        "these",
        "they",
        "thing",
        "things",
        "this",
        "those",
        "thx",
        "to",
        "too",
        "up",
        "us",
        "using",
        "very",
        "via",
        "wanna",
        "was",
        "we",
        "were",
        "what",
        "when",
        "where",
        "which",
        "who",
        "why",
        "will",
        "with",
        "would",
        "you",
        "your",
    )

    private val STOP_AND_WRAPPERS: Set<String> =
        ASK_VERBS + ASK_TYPE_NOUNS + FUNCTION_WORDS + RESULT_QUANTITY + setOf(
            "associated",
            "concerning",
            "containing",
            "example",
            "examples",
            "including",
            "involving",
            "like",
            "regarding",
            "related",
            "relating",
            "versus",
            "vs",
            "about",
            "from",
        )

    fun tokens(rawQuery: String): List<String> {
        val prepared = EG_ABBREV.replace(rawQuery.lowercase(), " eg ")
        return TOKEN_SPLIT.split(prepared)
            .map { it.trim() }
            .filter { it.length >= MIN_TOKEN_LENGTH && it !in STOP_AND_WRAPPERS }
    }
}
