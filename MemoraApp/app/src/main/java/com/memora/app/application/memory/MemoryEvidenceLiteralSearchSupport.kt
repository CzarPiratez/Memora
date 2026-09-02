package com.memora.app.application.memory

import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.RecallQueryContentTokens

/**
 * Pure helpers for MIG-06 literal search over [com.memora.app.domain.memory.MemoryEvidence]
 * excerpts.
 *
 * Keyword / substring only. Not semantic recall and not Canonical Recall.
 * Intentionally separate from per-asset `*KeywordSearchSupport` so L1–L4 are not
 * extended by this additive path.
 */
internal object MemoryEvidenceLiteralSearchSupport {
    const val MAX_QUERY_LENGTH = 120
    const val MAX_RESULTS = 20
    const val EXCERPT_RADIUS = 40

    /**
     * Over-fetch per token when intersecting multi-word AND matches per asset.
     */
    const val MULTI_TOKEN_SEARCH_POOL_MULTIPLIER = 5

    fun normalizeQuery(raw: String): String? {
        val collapsed = raw.trim().replace(Regex("""\s+"""), " ")
        if (collapsed.isEmpty()) return null
        return collapsed.take(MAX_QUERY_LENGTH)
    }

    /** Content tokens for multi-word AND keyword search (punctuation + stop words removed). */
    fun queryTokens(normalizedQuery: String): List<String> =
        RecallQueryContentTokens.tokens(normalizedQuery)

    fun assetKey(sourceId: SourceId, sourceAssetKey: SourceAssetKey): String =
        "${sourceId.value}\u001f${sourceAssetKey.value}"

    fun excerptContainsToken(excerpt: String, token: String): Boolean =
        excerpt.contains(token, ignoreCase = true)

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

    fun excerptAroundMatch(
        storedExcerpt: String,
        needle: String,
        radius: Int = EXCERPT_RADIUS,
    ): String {
        val matchIndex = storedExcerpt.indexOf(needle, ignoreCase = true)
        if (matchIndex < 0) {
            return storedExcerpt.take(radius * 2).trim().ifEmpty { storedExcerpt.trim() }
        }
        val start = (matchIndex - radius).coerceAtLeast(0)
        val end = (matchIndex + needle.length + radius).coerceAtMost(storedExcerpt.length)
        val slice = storedExcerpt.substring(start, end).trim()
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < storedExcerpt.length) "…" else ""
        return prefix + slice + suffix
    }
}
