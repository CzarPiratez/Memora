package com.memora.app.domain.intelligence

/**
 * Bounded English inflection for recall matching (MF-1.1 / I1).
 *
 * Whole-word variants only: `timetable` ↔ `timetables`. Not a stemmer, not
 * synonyms (`silky` must not become `silk` / `smooth`).
 */
object EnglishRecallInflection {
    fun wholeWordVariants(token: String): Set<String> {
        val t = token.lowercase().trim()
        if (t.all(Char::isDigit) && t.isNotEmpty()) return setOf(t)
        if (t.length < RecallQueryContentTokens.MIN_TOKEN_LENGTH) return emptySet()
        val out = linkedSetOf(t)
        addPlural(t, out)
        addSingular(t, out)
        return out.filter { it.length >= RecallQueryContentTokens.MIN_TOKEN_LENGTH }.toSet()
    }

    fun occursAsWholeWord(haystackLowercased: String, token: String): Boolean =
        wholeWordVariants(token).any { variant ->
            Regex("""\b${Regex.escape(variant)}\b""").containsMatchIn(haystackLowercased)
        }

    private fun addPlural(t: String, out: MutableSet<String>) {
        when {
            t.endsWith("s") || t.endsWith("x") || t.endsWith("z") ||
                t.endsWith("ch") || t.endsWith("sh") -> out += t + "es"
            t.endsWith("y") && t.length >= 3 && t[t.length - 2] !in "aeiou" ->
                out += t.dropLast(1) + "ies"
            else -> out += t + "s"
        }
    }

    private fun addSingular(t: String, out: MutableSet<String>) {
        when {
            t.endsWith("ies") && t.length >= 5 -> out += t.dropLast(3) + "y"
            t.endsWith("ches") || t.endsWith("shes") || t.endsWith("xes") ||
                t.endsWith("zes") || t.endsWith("ses") ->
                if (t.length >= 5) out += t.dropLast(2)
            t.endsWith("s") && !t.endsWith("ss") && t.length >= 4 ->
                out += t.dropLast(1)
        }
    }
}
