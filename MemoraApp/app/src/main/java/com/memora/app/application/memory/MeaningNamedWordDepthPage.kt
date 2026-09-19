package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue
import com.memora.app.domain.intelligence.UnfyndSelfCapture

/**
 * Shown-page occupancy among named-word **families** already eligible for
 * the trusted page (D-23–D-28).
 *
 * Pictures of UNFYND itself (D-14) can OCR every named word because the
 * Find chrome repeats the cue. They must not form the Exact family or they
 * undo [AnchorAwareMeaningRecallRanking] demotion. They stay last.
 *
 * The deepest in-band family occupies first (D-26 Exact is the case where
 * that depth is every named word). Shallower families fill leftover seats
 * (D-20 keep). When several families share that deepest depth, the starved
 * one takes its fair share first (D-25). Wifi / scan silky stay there —
 * every family is one word.
 *
 * A longer P-AND cue (`swimming classes for grade 2`) must not give
 * `{grade}`-only files an equal share beside a two-word swimming hit.
 * Not a type quota, not a synonym, not a bigger dump.
 */
object MeaningNamedWordDepthPage {
    fun order(hits: List<MeaningSearchHit>, rawQuery: String): List<MeaningSearchHit> {
        if (hits.size <= 1) return hits
        val selfCaptures = hits.filter { hit ->
            UnfyndSelfCapture.matches(hit.label, hit.lexicalHaystack())
        }
        val originals = if (selfCaptures.isEmpty()) {
            hits
        } else {
            hits.filterNot { hit ->
                UnfyndSelfCapture.matches(hit.label, hit.lexicalHaystack())
            }
        }
        if (originals.isEmpty()) return hits
        val ordered = orderOriginals(originals, rawQuery)
        return if (selfCaptures.isEmpty()) ordered else ordered + selfCaptures
    }

    private fun orderOriginals(
        hits: List<MeaningSearchHit>,
        rawQuery: String,
    ): List<MeaningSearchHit> {
        if (hits.size <= 1) return hits
        val tokens = MeaningRecallCue.contentTokens(rawQuery)
        if (tokens.size < 2) return hits
        val cue = MeaningEvidenceLexicalFilter.prepare(tokens)
        if (cue.isEmpty) return hits

        val grouped = LinkedHashMap<Set<String>, MutableList<MeaningSearchHit>>()
        val unmatched = mutableListOf<MeaningSearchHit>()
        for (hit in hits) {
            val matched = cue.matchingTokens(hit.lexicalHaystack()).toSet()
            if (matched.isEmpty()) {
                unmatched.add(hit)
                continue
            }
            grouped.getOrPut(matched) { mutableListOf() }.add(hit)
        }
        if (grouped.size <= 1) return hits

        val seen = grouped.keys.toList()
        val maxDepth = seen.maxOf { it.size }
        val deepKeys = seen.filter { it.size == maxDepth }
        val deepOrdered = if (deepKeys.size == 1) {
            grouped.getValue(deepKeys.single())
        } else {
            starvedMix(deepKeys, grouped, hits, cue)
        }
        val shallower = hits.filter { hit ->
            val matched = cue.matchingTokens(hit.lexicalHaystack()).toSet()
            matched.isNotEmpty() && matched.size < maxDepth
        }
        return deepOrdered + shallower + unmatched
    }

    private fun starvedMix(
        families: List<Set<String>>,
        grouped: Map<Set<String>, List<MeaningSearchHit>>,
        hits: List<MeaningSearchHit>,
        cue: MeaningEvidenceLexicalFilter.PreparedCue,
    ): List<MeaningSearchHit> {
        val prefix = hits.take(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS)
        val orderedFamilies = families.sortedWith(
            compareBy<Set<String>> { family ->
                prefix.count { hit ->
                    cue.matchingTokens(hit.lexicalHaystack()).toSet() == family
                }
            }.thenBy { family -> families.indexOf(family) },
        )
        val queues = orderedFamilies.associateWith { family ->
            ArrayDeque(grouped.getValue(family))
        }
        val mixed = ArrayList<MeaningSearchHit>(hits.size)
        val fairShare = (MeaningTrustedHitPolicy.MAX_TRUSTED_HITS / orderedFamilies.size)
            .coerceAtLeast(1)
        for (family in orderedFamilies) {
            var taken = 0
            while (taken < fairShare) {
                val next = queues.getValue(family).removeFirstOrNull() ?: break
                mixed.add(next)
                taken += 1
            }
        }
        while (queues.any { it.value.isNotEmpty() }) {
            for (family in orderedFamilies) {
                val next = queues.getValue(family).removeFirstOrNull() ?: continue
                mixed.add(next)
            }
        }
        return mixed
    }
}
