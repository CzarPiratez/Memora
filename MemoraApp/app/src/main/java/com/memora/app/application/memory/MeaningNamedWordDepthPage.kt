package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallRoles
import com.memora.app.domain.intelligence.UnfyndSelfCapture

/**
 * Shown-page order for meaning Find (ADR-055 slice 1).
 *
 * One comparator, not an occupancy case table:
 * 1. Pictures of UNFYND stay last (D-14 / D-27).
 * 2. Any head-word hit beats qualifier-only hits.
 * 3. More qualifier overlap beats less (narrows the same job).
 * 4. More head overlap beats less (Exact head leads).
 * 5. Same tier, not Exact head → fair share among head families (wifi /
 *    scan silky). Same tier, Exact head → incoming / cosine order.
 *
 * Not a type quota. Not a synonym. Embed text is unchanged (D-10).
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

        val headCue = MeaningEvidenceLexicalFilter.prepare(roles.head)
        val qualCue = MeaningEvidenceLexicalFilter.prepare(roles.qualifier)
        val scored = hits.map { hit ->
            val haystack = hit.lexicalHaystack()
            val headMatched = headCue.matchingTokens(haystack).toSet()
            val qualMatched = if (qualCue.isEmpty) {
                emptySet()
            } else {
                qualCue.matchingTokens(haystack).toSet()
            }
            Scored(
                hit = hit,
                hasHead = headMatched.isNotEmpty(),
                headOverlap = headMatched.size,
                qualifierOverlap = qualMatched.size,
                family = when {
                    headMatched.isNotEmpty() -> headMatched
                    qualMatched.isNotEmpty() -> qualMatched
                    else -> emptySet()
                },
            )
        }
        val unmatched = scored.filter { it.family.isEmpty() }.map { it.hit }
        val named = scored.filter { it.family.isNotEmpty() }
        if (named.isEmpty()) return hits

        val tiers = named.groupBy { it.tierKey() }
            .toSortedMap(tierComparator)
        val ordered = ArrayList<MeaningSearchHit>(hits.size)
        for ((_, rows) in tiers) {
            ordered.addAll(orderTier(rows, roles.head.size))
        }
        ordered.addAll(unmatched)
        return ordered
    }

    private fun orderTier(rows: List<Scored>, headSize: Int): List<MeaningSearchHit> {
        val families = LinkedHashMap<Set<String>, MutableList<MeaningSearchHit>>()
        for (row in rows) {
            families.getOrPut(row.family) { mutableListOf() }.add(row.hit)
        }
        if (families.size <= 1) return rows.map { it.hit }
        val exactHead = rows.first().hasHead &&
            rows.first().headOverlap == headSize &&
            headSize >= 2
        if (exactHead) return rows.map { it.hit }
        return fairShare(rows, families)
    }

    private fun fairShare(
        rows: List<Scored>,
        families: Map<Set<String>, List<MeaningSearchHit>>,
    ): List<MeaningSearchHit> {
        val keys = families.keys.toList()
        val prefix = rows.take(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS)
        val orderedKeys = keys.sortedWith(
            compareBy<Set<String>> { family ->
                prefix.count { it.family == family }
            }.thenBy { family -> keys.indexOf(family) },
        )
        val queues = orderedKeys.associateWith { family ->
            ArrayDeque(families.getValue(family))
        }
        val mixed = ArrayList<MeaningSearchHit>()
        val fairShare = (MeaningTrustedHitPolicy.MAX_TRUSTED_HITS / orderedKeys.size)
            .coerceAtLeast(1)
        for (family in orderedKeys) {
            var taken = 0
            while (taken < fairShare) {
                val next = queues.getValue(family).removeFirstOrNull() ?: break
                mixed.add(next)
                taken += 1
            }
        }
        while (queues.any { it.value.isNotEmpty() }) {
            for (family in orderedKeys) {
                val next = queues.getValue(family).removeFirstOrNull() ?: continue
                mixed.add(next)
            }
        }
        return mixed
    }

    private data class Scored(
        val hit: MeaningSearchHit,
        val hasHead: Boolean,
        val headOverlap: Int,
        val qualifierOverlap: Int,
        val family: Set<String>,
    ) {
        fun tierKey(): TierKey = TierKey(hasHead, qualifierOverlap, headOverlap)
    }

    private data class TierKey(
        val hasHead: Boolean,
        val qualifierOverlap: Int,
        val headOverlap: Int,
    )

    private val tierComparator: Comparator<TierKey> =
        compareByDescending<TierKey> { it.hasHead }
            .thenByDescending { it.qualifierOverlap }
            .thenByDescending { it.headOverlap }
}
