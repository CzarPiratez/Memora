package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.EnglishRecallInflection
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue

/**
 * Meaning-Find Why — what a person actually asks: why is *this file*
 * relevant to what I was trying to remember?
 *
 * Not a word inventory. "Has swimming. Does not have schedule." is an audit
 * of the lexical gate; it is not an explanation (defect D-17 follow-on).
 * The sentence pairs their ask with what the file calls itself, using only
 * stored text and the filename. It never claims `schedule` means `timetable`.
 *
 * Missing-word honesty stays on the result-list banner, not on every card.
 * The justifying line lives here because the card no longer dumps OCR.
 */
object MeaningWhy {
    data class Explanation(
        val asked: String,
        val fileIs: String,
        val citedLine: String?,
        val matched: List<String>,
        val missing: List<String>,
    ) {
        init {
            require(asked.isNotBlank())
            require(fileIs.isNotBlank())
        }
    }

    fun explain(hit: MeaningSearchHit, query: String): Explanation {
        val named = MeaningRecallCue.contentTokens(query)
        val matched = MeaningEvidenceLexicalFilter.prepare(named)
            .matchingTokens(hit.lexicalHaystack())
        return Explanation(
            asked = askedPhrase(query),
            fileIs = fileIs(hit),
            citedLine = justifyingLine(hit, matched),
            matched = matched,
            missing = named - matched.toSet(),
        )
    }

    fun relevanceText(asked: String, fileIs: String): String =
        "You asked about $asked. This file is $fileIs."

    fun plainText(explanation: Explanation): String = buildList {
        add(relevanceText(explanation.asked, explanation.fileIs))
        explanation.citedLine?.let { add("\"$it\"") }
    }.joinToString("\n")

    fun askedPhrase(query: String): String {
        val tokens = MeaningRecallCue.contentTokens(query)
        val phrase = if (tokens.isEmpty()) {
            MeaningRecallCue.displayQuery(query)
        } else {
            tokens.joinToString(" ")
        }
        val first = phrase.firstOrNull()?.lowercaseChar()
        val article = if (first != null && first in "aeiou") "an " else "a "
        return article + phrase
    }

    fun fileIs(hit: MeaningSearchHit): String =
        humanizeFilename(hit.label) ?: firstReadableClause(hit.summaryText)

    fun fileIs(label: String, excerpt: String): String =
        humanizeFilename(label) ?: firstReadableClause(excerpt)

    internal fun humanizeFilename(label: String): String? {
        val friendly = CanonicalRecallWhyCopy.friendlyDisplayLabel(label)
        val stem = friendly.substringBeforeLast('.').trim()
        if (stem.isEmpty()) return null
        if (MACHINE_NAME.matches(stem)) return null
        val words = stem.replace(DASHES, " ").replace(WHITESPACE, " ").trim()
        if (words.length < 3) return null
        return words
    }

    internal fun firstReadableClause(text: String): String {
        val words = text.trim().replace(WHITESPACE, " ").split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return "a saved file"
        val cut = words.take(10).takeWhileIndexed { index, word ->
            index == 0 || !isScheduleHeaderNoise(word)
        }
        return cut.joinToString(" ").trimEnd('.', ',', ';', ':')
    }

    private fun justifyingLine(hit: MeaningSearchHit, matched: List<String>): String? {
        val word = matched.firstOrNull()
        if (word != null) {
            windowAround(hit.lexicalHaystack(), word)?.let { return it }
        }
        val clause = firstReadableClause(hit.summaryText)
        return clause.takeIf { it != "a saved file" }
    }

    internal fun windowAround(text: String, token: String, maxChars: Int = 72): String? {
        val lower = text.lowercase()
        val match = EnglishRecallInflection.wholeWordVariants(token)
            .firstNotNullOfOrNull { variant ->
                Regex("""\b${Regex.escape(variant)}\b""").find(lower)
            } ?: return null
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

    private fun isScheduleHeaderNoise(word: String): Boolean {
        val t = word.trim().uppercase()
        return t in HEADER_NOISE || (t.length <= 3 && t.all { it.isLetter() } && t == word.uppercase() && t in setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "P1", "P2", "P3"))
    }

    private inline fun <T> List<T>.takeWhileIndexed(predicate: (Int, T) -> Boolean): List<T> {
        val out = ArrayList<T>()
        forEachIndexed { index, item ->
            if (!predicate(index, item)) return out
            out += item
        }
        return out
    }

    private val DASHES = Regex("""[-_]+""")
    private val WHITESPACE = Regex("""\s+""")
    private val MACHINE_NAME = Regex(
        """^(Screenshot|IMG|image|VID|PXL)[_-]?\d.*""",
        RegexOption.IGNORE_CASE,
    )
    private val HEADER_NOISE = setOf("PERIOD", "TIME", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
}
