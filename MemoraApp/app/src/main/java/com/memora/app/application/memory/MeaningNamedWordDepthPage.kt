package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue

/**
 * Shown-page occupancy among named-word depths already in the trusted band
 * (D-23).
 *
 * D-21 keeps every named-word family in the admitted 60. Token boost then
 * saturates many of those hits at 1.0, so the page falls back to cosine order
 * and one depth can occupy all 20 seats. A 1-token swimming neighbour that is
 * already in the band (founder: admitted 28, ranked 21, shown missed) never
 * appears.
 *
 * This is not a type quota and not a bigger dump. One-word cues and a single
 * depth stay in incoming order. Deepest depth still leads each round.
 */
object MeaningNamedWordDepthPage {
    fun order(hits: List<MeaningSearchHit>, rawQuery: String): List<MeaningSearchHit> {
        if (hits.size <= 1) return hits
        val tokens = MeaningRecallCue.contentTokens(rawQuery)
        if (tokens.size < 2) return hits
        val cue = MeaningEvidenceLexicalFilter.prepare(tokens)
        if (cue.isEmpty) return hits

        val grouped = LinkedHashMap<Int, MutableList<MeaningSearchHit>>()
        for (hit in hits) {
            val depth = cue.matchingTokens(hit.lexicalHaystack()).size
            grouped.getOrPut(depth) { mutableListOf() }.add(hit)
        }
        val depths = grouped.keys.filter { it > 0 }.sortedDescending()
        if (depths.size <= 1) return hits

        val queues = depths.associateWith { depth -> ArrayDeque(grouped.getValue(depth)) }
        val mixed = ArrayList<MeaningSearchHit>(hits.size)
        while (queues.any { it.value.isNotEmpty() }) {
            for (depth in depths) {
                val next = queues.getValue(depth).removeFirstOrNull() ?: continue
                mixed.add(next)
            }
        }
        mixed.addAll(grouped[0].orEmpty())
        return mixed
    }
}
