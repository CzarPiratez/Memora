package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue

/**
 * Shown-page occupancy among named-word **families** already in the trusted
 * band (D-23 depth mix, D-24 family mix).
 *
 * D-21 keeps every named-word family in the admitted 60. D-23 mixed by token
 * *count*. Founder re-test: the timetable PDF was still missing because the
 * files above it were also 1-token (classes-only) with higher cosine — one
 * depth, so D-23 was a no-op and the cap still hid `{swimming}`.
 *
 * Grouping key is the matching token **set**, not the count. `{swimming,
 * classes}`, `{swimming}`, and `{classes}` share the 20. Not a type quota,
 * not a synonym (`classes` ≠ `timetable`), not a bigger dump. One-word cues
 * and a single family stay in incoming order. Deeper sets still lead each
 * round.
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
        val families = seen.sortedWith(
            compareByDescending<Set<String>> { it.size }
                .thenBy { family -> seen.indexOf(family) },
        )
        val queues = families.associateWith { family -> ArrayDeque(grouped.getValue(family)) }
        val mixed = ArrayList<MeaningSearchHit>(hits.size)
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
