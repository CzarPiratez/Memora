package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue

/**
 * Shown-page occupancy among named-word **families** already in the trusted
 * band (D-23 / D-24 / D-25).
 *
 * D-24 round-robins token sets so `{swimming}` can appear beside `{classes}`.
 * Founder: timetables landed 8th and 14th — `{classes}` still led every
 * round. When no file has every named word, the family that is starved in
 * the cosine prefix takes its fair share first. Exact / deepest still leads
 * when some file has every word. Not a type quota, not a synonym, not a
 * bigger dump.
 */
object MeaningNamedWordDepthPage {
    fun order(hits: List<MeaningSearchHit>, rawQuery: String): List<MeaningSearchHit> {
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
        val hasExactFamily = seen.any { it.size == tokens.size }
        val families = if (hasExactFamily) {
            seen.sortedWith(
                compareByDescending<Set<String>> { it.size }
                    .thenBy { family -> seen.indexOf(family) },
            )
        } else {
            val prefix = hits.take(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS)
            seen.sortedWith(
                compareBy<Set<String>> { family ->
                    prefix.count { hit ->
                        cue.matchingTokens(hit.lexicalHaystack()).toSet() == family
                    }
                }.thenBy { family -> seen.indexOf(family) },
            )
        }
        val queues = families.associateWith { family -> ArrayDeque(grouped.getValue(family)) }
        val mixed = ArrayList<MeaningSearchHit>(hits.size)
        if (!hasExactFamily) {
            val fairShare = (MeaningTrustedHitPolicy.MAX_TRUSTED_HITS / families.size)
                .coerceAtLeast(1)
            for (family in families) {
                var taken = 0
                while (taken < fairShare) {
                    val next = queues.getValue(family).removeFirstOrNull() ?: break
                    mixed.add(next)
                    taken += 1
                }
            }
        }
        while (queues.any { it.value.isNotEmpty() }) {
            for (family in families) {
                val next = queues.getValue(family).removeFirstOrNull() ?: continue
                mixed.add(next)
            }
        }
        mixed.addAll(unmatched)
        return mixed
    }
}
