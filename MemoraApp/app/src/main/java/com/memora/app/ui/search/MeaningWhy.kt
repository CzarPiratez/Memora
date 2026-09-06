package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.EnglishRecallInflection
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue

/**
 * Meaning-Find Why (scenario bar U5 + P-EVIDENCE).
 *
 * The card already shows the file name, type, page, and snippet. Why answers
 * one question those do not: **which of the person's words are in this file**.
 * On a partial tier it must also name the words that are not — "your cue words
 * appear in that saved text" is a lie when `schedule` never did (defect D-17).
 *
 * A cited line is included only when the matching word is *not* already
 * visible on the card. Repeating the cosine snippet the card shows is the U5
 * anti-pattern; quoting a different stored span is how Why earns its space
 * (the start of D-7).
 */
object MeaningWhy {
    data class Explanation(
        val matched: List<String>,
        val missing: List<String>,
        val citedLine: String?,
    ) {
        init {
            require(matched.isEmpty() || matched.all { it.isNotBlank() })
            require(missing.all { it.isNotBlank() })
        }
    }

    fun explain(hit: MeaningSearchHit, query: String): Explanation {
        val named = MeaningRecallCue.contentTokens(query)
        val matched = MeaningEvidenceLexicalFilter.prepare(named)
            .matchingTokens(hit.lexicalHaystack())
        return Explanation(
            matched = matched,
            missing = named - matched.toSet(),
            citedLine = citedLineIfHiddenOnCard(hit, matched),
        )
    }

    fun coverageText(matched: List<String>, missing: List<String>): String {
        val has = if (matched.isEmpty()) {
            "None of those words are in this file."
        } else {
            "Has ${quoteWords(matched)}."
        }
        if (missing.isEmpty()) return has
        return "$has Does not have ${quoteWords(missing)}."
    }

    fun plainText(explanation: Explanation): String = buildList {
        add(coverageText(explanation.matched, explanation.missing))
        explanation.citedLine?.let { add("On this line: \"$it\"") }
    }.joinToString("\n")

    fun quoteWords(words: List<String>): String {
        val quoted = words.map { "\"$it\"" }
        return when (quoted.size) {
            0 -> ""
            1 -> quoted.first()
            else -> quoted.dropLast(1).joinToString(", ") + " and " + quoted.last()
        }
    }

    /**
     * Quote a stored span only when the card snippet does not already carry
     * the first matched word. The haystack is the whole Memory (P-EVIDENCE).
     */
    private fun citedLineIfHiddenOnCard(
        hit: MeaningSearchHit,
        matched: List<String>,
    ): String? {
        val word = matched.firstOrNull() ?: return null
        if (MeaningEvidenceLexicalFilter.satisfies(listOf(word), hit.summaryText)) {
            return null
        }
        return windowAround(hit.lexicalHaystack(), word)
    }

    internal fun windowAround(text: String, token: String, maxChars: Int = 72): String? {
        val lower = text.lowercase()
        val match = EnglishRecallInflection.wholeWordVariants(token)
            .firstNotNullOfOrNull { variant ->
                Regex("""\b${Regex.escape(variant)}\b""").find(lower)
            } ?: return null
        // Keep the word near the start of the quote so Why does not look like
        // the cosine snippet it just refused to duplicate.
        val left = 12
        val right = (maxChars - (match.range.last - match.range.first + 1) - left)
            .coerceAtLeast(24)
        val start = (match.range.first - left).coerceAtLeast(0)
        val end = (match.range.last + 1 + right).coerceAtMost(text.length)
        val slice = text.substring(start, end).replace(WHITESPACE, " ").trim()
        if (slice.isEmpty()) return null
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return prefix + slice + suffix
    }

    private val WHITESPACE = Regex("""\s+""")
}
