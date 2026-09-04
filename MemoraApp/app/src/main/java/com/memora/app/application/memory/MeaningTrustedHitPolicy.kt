package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit

/**
 * Trust policy for meaning result lists (scenario bar MF-1).
 *
 * Prefer a short band near the top score over padding to a fixed top-10 of
 * weak neighbors.
 */
object MeaningTrustedHitPolicy {
    const val MAX_TRUSTED_HITS = 5
    const val RELATIVE_SCORE_GAP = 0.22f

    fun apply(hits: List<MeaningSearchHit>, limit: Int): List<MeaningSearchHit> {
        if (hits.isEmpty()) return hits
        val cappedLimit = limit.coerceAtMost(MAX_TRUSTED_HITS).coerceAtLeast(1)
        val topScore = hits.first().score
        val inBand = hits.filter { hit ->
            topScore - hit.score <= RELATIVE_SCORE_GAP
        }
        return inBand.take(cappedLimit)
    }
}
