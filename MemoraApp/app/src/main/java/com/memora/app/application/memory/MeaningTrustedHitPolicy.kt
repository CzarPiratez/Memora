package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit

/**
 * Trust policy for meaning result lists (D-20 / MF-1).
 *
 * One relevance order for every asset type. No reserved seats for Partial
 * hits, PDFs, notes, or a particular cue. Coverage ranking lives in
 * [AnchorAwareMeaningRecallRanking]; this stage only trims the shown page.
 *
 * The band reads raw [MeaningSearchHit.cosine], not the boosted working
 * [MeaningSearchHit.score]. Token boost was making Exact files sit at 1.0 and
 * then deleting neighbours the model had already retrieved.
 *
 * The shown page is **up to** [MAX_TRUSTED_HITS] — a cap, not a floor. A thin
 * close band stays thin; do not pad. A larger corpus must improve ranking
 * inside Canonical Recall, not grow this dump. Raising the cap is a product
 * decision with its own change control, not an automatic scale-with-n rule.
 */
object MeaningTrustedHitPolicy {
    const val MAX_TRUSTED_HITS = 20
    const val RELATIVE_SCORE_GAP = 0.22f

    data class Page(
        val hits: List<MeaningSearchHit>,
        val truncatedByPageCap: Boolean,
    )

    fun apply(hits: List<MeaningSearchHit>, limit: Int): List<MeaningSearchHit> =
        page(hits, limit).hits

    fun page(hits: List<MeaningSearchHit>, limit: Int): Page {
        if (hits.isEmpty()) return Page(hits, truncatedByPageCap = false)
        val cappedLimit = limit.coerceAtMost(MAX_TRUSTED_HITS).coerceAtLeast(1)
        val topCosine = hits.maxOf { it.cosine }
        val inBand = hits.filter { hit ->
            topCosine - hit.cosine <= RELATIVE_SCORE_GAP
        }
        return Page(
            hits = inBand.take(cappedLimit),
            truncatedByPageCap = inBand.size > cappedLimit,
        )
    }

    /** Same trim as [page]; the query does not change seating or asset type. */
    fun apply(
        hits: List<MeaningSearchHit>,
        limit: Int,
        rawQuery: String,
    ): List<MeaningSearchHit> {
        if (rawQuery.isBlank()) return apply(hits, limit)
        return apply(hits, limit)
    }
}
