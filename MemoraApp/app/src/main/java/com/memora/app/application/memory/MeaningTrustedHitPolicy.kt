package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue

/**
 * Trust policy for meaning result lists (scenario bar MF-1).
 *
 * Prefer a short band near the top score over padding to a fixed top-10 of
 * weak neighbors. When Exact and Partial hits share a list (defect D-20),
 * that band must not be measured against the Exact top score: token boost
 * caps Exact files at 1.0 and a timetable at rank ~380 then falls outside
 * 0.22 even though ranking already admitted it.
 */
object MeaningTrustedHitPolicy {
    const val MAX_TRUSTED_HITS = 5
    const val RELATIVE_SCORE_GAP = 0.22f

    /**
     * Seats reserved for Partial hits when Exact competitors exist, so the
     * shown five cannot be Exact-only. Leaves at least one seat for Exact.
     */
    const val RESERVED_PARTIAL_WHEN_MIXED = 2

    fun apply(hits: List<MeaningSearchHit>, limit: Int): List<MeaningSearchHit> {
        if (hits.isEmpty()) return hits
        val cappedLimit = limit.coerceAtMost(MAX_TRUSTED_HITS).coerceAtLeast(1)
        val topScore = hits.first().score
        val inBand = hits.filter { hit ->
            topScore - hit.score <= RELATIVE_SCORE_GAP
        }
        return inBand.take(cappedLimit)
    }

    fun apply(
        hits: List<MeaningSearchHit>,
        limit: Int,
        rawQuery: String,
    ): List<MeaningSearchHit> {
        if (hits.isEmpty()) return hits
        val required = MeaningRecallCue.contentTokens(rawQuery)
        if (required.size < 2) return apply(hits, limit)

        val cue = MeaningEvidenceLexicalFilter.prepare(required)
        val exact = hits.filter { hit ->
            cue.matchingTokens(hit.lexicalHaystack()).size == required.size
        }
        val partial = hits.filter { hit ->
            val matched = cue.matchingTokens(hit.lexicalHaystack()).size
            matched in 1 until required.size
        }
        if (exact.isEmpty() || partial.isEmpty()) return apply(hits, limit)

        val cap = limit.coerceAtMost(MAX_TRUSTED_HITS).coerceAtLeast(1)
        val reservedCount = minOf(RESERVED_PARTIAL_WHEN_MIXED, partial.size, cap - 1)
        val reservedPartial = partial.take(reservedCount)
        val exactKeep = apply(exact, cap - reservedPartial.size)
        val keepIds = (exactKeep + reservedPartial).map { it.revisionId }.toHashSet()
        val leftover = cap - keepIds.size
        if (leftover > 0) {
            partial.asSequence()
                .filter { it.revisionId !in keepIds }
                .take(leftover)
                .forEach { keepIds.add(it.revisionId) }
        }
        return hits.filter { it.revisionId in keepIds }
    }
}
