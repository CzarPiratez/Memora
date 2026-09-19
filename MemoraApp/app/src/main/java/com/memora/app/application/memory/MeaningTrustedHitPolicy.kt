package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue

/**
 * Trust policy for meaning result lists (D-20 / MF-1).
 *
 * One relevance order for every asset type. No reserved seats for PDFs,
 * notes, or a particular cue. Eligible hits are ordered by
 * [MeaningNamedWordDepthPage] (ADR-055: head / qualifier / one score).
 * Pictures of UNFYND (D-14) stay last. That is not a type quota. Coverage
 * ranking lives in [AnchorAwareMeaningRecallRanking]; this stage trims the
 * shown page.
 *
 * The band reads raw [MeaningSearchHit.cosine], not the boosted working
 * [MeaningSearchHit.score]. Token boost was making Exact files sit at 1.0 and
 * then deleting neighbours the model had already retrieved.
 *
 * A two-or-more-word hit stays eligible even when one-word files set a
 * higher cosine (D-28). One-word cues still use the close band alone.
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

    fun page(hits: List<MeaningSearchHit>, limit: Int, rawQuery: String = ""): Page {
        if (hits.isEmpty()) return Page(hits, truncatedByPageCap = false)
        val cappedLimit = limit.coerceAtMost(MAX_TRUSTED_HITS).coerceAtLeast(1)
        val topCosine = hits.maxOf { it.cosine }
        val close = hits.filter { hit ->
            topCosine - hit.cosine <= RELATIVE_SCORE_GAP
        }
        val eligible = eligibleHits(hits, close, rawQuery)
        val ordered = MeaningNamedWordDepthPage.order(eligible, rawQuery)
        return Page(
            hits = ordered.take(cappedLimit),
            truncatedByPageCap = eligible.size > cappedLimit,
        )
    }

    private fun eligibleHits(
        hits: List<MeaningSearchHit>,
        close: List<MeaningSearchHit>,
        rawQuery: String,
    ): List<MeaningSearchHit> {
        val tokens = MeaningRecallCue.contentTokens(rawQuery)
        if (tokens.size < 2) return close
        val cue = MeaningEvidenceLexicalFilter.prepare(tokens)
        if (cue.isEmpty) return close
        val maxDepth = hits.maxOf { cue.matchingTokens(it.lexicalHaystack()).size }
        if (maxDepth < 2) return close
        val closeIds = close.map { it.revisionId }.toHashSet()
        return hits.filter { hit ->
            hit.revisionId in closeIds ||
                cue.matchingTokens(hit.lexicalHaystack()).size == maxDepth
        }
    }

    /**
     * Same trim as [page]. [rawQuery] may mix named-word families on a capped
     * page. It does not reserve seats by asset type.
     */
    fun apply(
        hits: List<MeaningSearchHit>,
        limit: Int,
        rawQuery: String,
    ): List<MeaningSearchHit> = page(hits, limit, rawQuery).hits
}
