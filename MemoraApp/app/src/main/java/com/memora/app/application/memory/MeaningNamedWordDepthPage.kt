package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.MeaningRecallRoles
import com.memora.app.domain.intelligence.MeaningRoleScorer
import com.memora.app.domain.intelligence.UnfyndSelfCapture

/**
 * Shown-page order for meaning Find (ADR-055 slice 1).
 *
 * One comparator, not an occupancy case table:
 * 1. Pictures of UNFYND stay last (D-14 / D-27).
 * 2. Job cues (ask-shape or a constraint): topic (first job word) leads;
 *    leftover job words cannot take the first seats. Inside the topic
 *    band, a tighter constraint outranks extra leftover head words.
 * 3. Bare list cues: more head-word overlap, then family fair share.
 * 4. Exact head, one family, no constraint → incoming / cosine order.
 *
 * Not a type quota. Not a synonym. Not a per-cue table. Embed text is
 * unchanged (D-10). Device phrasings are specimens.
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
        val roles = MeaningRecallRoles.parse(rawQuery)
        if (roles.head.size < 2 && roles.qualifier.isEmpty()) return hits

        val scorer = MeaningRoleScorer.forQuery(rawQuery)
        val scored = hits.map { hit ->
            val overlap = scorer.overlap(hit.lexicalHaystack())
            Scored(
                hit = hit,
                headOverlap = overlap.headOverlap,
                qualifierOverlap = overlap.qualifierOverlap,
                family = overlap.family,
                band = scorer.band(overlap),
            )
        }
        val unmatched = scored.filter { it.family.isEmpty() }.map { it.hit }
        val named = scored.filter { it.family.isNotEmpty() }
        if (named.isEmpty()) return hits

        val tiers = named.groupBy { it.band }
            .toSortedMap(compareByDescending { it })
        val ordered = ArrayList<MeaningSearchHit>(hits.size)
        for ((_, rows) in tiers) {
            ordered.addAll(
                if (scorer.preferTopic) {
                    orderConstrainedTier(rows)
                } else {
                    orderTier(rows, roles.head.size)
                },
            )
        }
        ordered.addAll(unmatched)
        return ordered
    }

    private fun orderConstrainedTier(rows: List<Scored>): List<MeaningSearchHit> =
        rows.sortedWith(
            compareByDescending<Scored> { it.qualifierOverlap }
                .thenByDescending { it.headOverlap },
        ).map { it.hit }

    private fun orderTier(rows: List<Scored>, headSize: Int): List<MeaningSearchHit> {
        val families = LinkedHashMap<Set<String>, MutableList<Scored>>()
        for (row in rows) {
            families.getOrPut(row.family) { mutableListOf() }.add(row)
        }
        if (families.size <= 1) {
            return rows.sortedByDescending { it.qualifierOverlap }.map { it.hit }
        }
        val exactHead = rows.first().headOverlap == headSize && headSize >= 2
        if (exactHead) return rows.map { it.hit }
        return fairShare(rows, families)
    }

    private fun fairShare(
        rows: List<Scored>,
        families: Map<Set<String>, List<Scored>>,
    ): List<MeaningSearchHit> {
        val keys = families.keys.toList()
        val prefix = rows.take(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS)
        val orderedKeys = keys.sortedWith(
            compareBy<Set<String>> { family ->
                prefix.count { it.family == family }
            }.thenBy { family -> keys.indexOf(family) },
        )
        val queues = orderedKeys.associateWith { family ->
            ArrayDeque(
                families.getValue(family).sortedByDescending { it.qualifierOverlap },
            )
        }
        val mixed = ArrayList<MeaningSearchHit>()
        val fairShare = (MeaningTrustedHitPolicy.MAX_TRUSTED_HITS / orderedKeys.size)
            .coerceAtLeast(1)
        for (family in orderedKeys) {
            var taken = 0
            while (taken < fairShare) {
                val next = queues.getValue(family).removeFirstOrNull() ?: break
                mixed.add(next.hit)
                taken += 1
            }
        }
        while (queues.any { it.value.isNotEmpty() }) {
            for (family in orderedKeys) {
                val next = queues.getValue(family).removeFirstOrNull() ?: continue
                mixed.add(next.hit)
            }
        }
        return mixed
    }

    private data class Scored(
        val hit: MeaningSearchHit,
        val headOverlap: Int,
        val qualifierOverlap: Int,
        val family: Set<String>,
        val band: Int,
    )
}
