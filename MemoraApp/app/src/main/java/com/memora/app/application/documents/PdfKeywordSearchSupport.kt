package com.memora.app.application.documents

/**
 * Pure helpers shared by interim on-device keyword search (highlight + L2–L4).
 *
 * PDF product Find no longer searches via these helpers (MIG-07 → MemoryEvidence),
 * but L2–L4 and PDF excerpt highlight still reuse normalize/escape/span helpers.
 */
internal object PdfKeywordSearchSupport {
    const val MAX_QUERY_LENGTH = 120
    const val MAX_RESULTS = 20
    const val EXCERPT_RADIUS = 40
    const val SCHEMA_VERSION = "pdf-extraction-v1"

    fun normalizeQuery(raw: String): String? {
        val collapsed = raw.trim().replace(Regex("""\s+"""), " ")
        if (collapsed.isEmpty()) return null
        return collapsed.take(MAX_QUERY_LENGTH)
    }

    /** Escapes `\`, `%`, and `_` so user input is treated literally in SQL LIKE. */
    fun escapeForLike(needle: String): String = buildString(needle.length) {
        for (ch in needle) {
            when (ch) {
                '\\', '%', '_' -> {
                    append('\\')
                    append(ch)
                }
                else -> append(ch)
            }
        }
    }

    fun excerptAroundMatch(pageText: String, needle: String, radius: Int = EXCERPT_RADIUS): String {
        val matchIndex = pageText.indexOf(needle, ignoreCase = true)
        if (matchIndex < 0) {
            return pageText.take(radius * 2).trim().ifEmpty { pageText.trim() }
        }
        val start = (matchIndex - radius).coerceAtLeast(0)
        val end = (matchIndex + needle.length + radius).coerceAtMost(pageText.length)
        val slice = pageText.substring(start, end).trim()
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < pageText.length) "…" else ""
        return prefix + slice + suffix
    }

    /**
     * First case-insensitive substring span of [needle] in [haystack], if any.
     *
     * Used to emphasize the same match already used for excerpt construction.
     */
    fun firstMatchSpan(haystack: String, needle: String): IntRange? {
        if (needle.isBlank() || haystack.isEmpty()) return null
        val start = haystack.indexOf(needle, ignoreCase = true)
        if (start < 0) return null
        val endExclusive = start + needle.length
        if (endExclusive > haystack.length) return null
        return start until endExclusive
    }
}
