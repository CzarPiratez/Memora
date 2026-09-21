package com.memora.app.domain.intelligence

/**
 * Overlap of a stored haystack against [MeaningRecallRoles].
 *
 * Vocabulary-free. Device cues are specimens, not this object's API.
 */
data class MeaningRoleOverlap(
    val headMatched: Set<String>,
    val qualifierMatched: Set<String>,
) {
    val headOverlap: Int get() = headMatched.size
    val qualifierOverlap: Int get() = qualifierMatched.size
    val family: Set<String>
        get() = if (headMatched.isNotEmpty()) headMatched else qualifierMatched
}

/**
 * Compiled once per query so pool admission and page order cannot drift.
 *
 * When the cue is a job (constraint or ask-shape wrappers), the first
 * remaining job word is the **topic**. Files that match the topic lead.
 * Leftover job words cannot take the first seats. Bare list cues keep
 * family fair share.
 */
class MeaningRoleScorer(
    roles: MeaningRecallRoles,
    val preferTopic: Boolean = roles.qualifier.isNotEmpty(),
) {
    val hasQualifier: Boolean = roles.qualifier.isNotEmpty()
    private val topic: String? = roles.head.firstOrNull()
    private val headCue = MeaningEvidenceLexicalFilter.prepare(roles.head)
    private val qualifierCue = MeaningEvidenceLexicalFilter.prepare(roles.qualifier)

    companion object {
        fun forQuery(rawQuery: String): MeaningRoleScorer {
            val roles = MeaningRecallRoles.parse(rawQuery)
            return MeaningRoleScorer(
                roles = roles,
                preferTopic = roles.qualifier.isNotEmpty() ||
                    MeaningRecallCue.hadAskShape(rawQuery),
            )
        }
    }

    fun overlap(haystack: String): MeaningRoleOverlap {
        val headMatched = headCue.matchingTokens(haystack).toSet()
        val qualifierMatched = if (qualifierCue.isEmpty) {
            emptySet()
        } else {
            qualifierCue.matchingTokens(haystack).toSet()
        }
        return MeaningRoleOverlap(headMatched, qualifierMatched)
    }

    /**
     * Higher is better. Without a constraint this is head-token count
     * (list / Exact cues). With a constraint: topic+constraint, then
     * topic, then leftover job words, then constraint-only.
     */
    fun band(overlap: MeaningRoleOverlap): Int {
        if (!preferTopic) return overlap.headOverlap
        val hasTopic = topic != null && topic in overlap.headMatched
        val hasQual = overlap.qualifierOverlap > 0
        return when {
            hasTopic && hasQual -> 3
            hasTopic -> 2
            overlap.headOverlap > 0 -> 1
            else -> 0
        }
    }
}

/**
 * Admit a bounded pool by [MeaningRoleScorer.band], then family fair share
 * only for unconstrained list cues (ADR-055).
 *
 * Cosine is a tie-break inside a family, not a veto. The returned list is
 * the original cosine order of the admitted set so candidate generation
 * still does not rank.
 */
object MeaningRoleAdmission {
    fun <T> take(
        items: List<T>,
        limit: Int,
        scorer: MeaningRoleScorer,
        haystackOf: (T) -> String,
    ): List<T> = take(
        items = items,
        limit = limit,
        overlapOf = { scorer.overlap(haystackOf(it)) },
        scorer = scorer,
    )

    fun <T> take(
        items: List<T>,
        limit: Int,
        overlapOf: (T) -> MeaningRoleOverlap,
        scorer: MeaningRoleScorer,
    ): List<T> {
        if (items.size <= limit) return items
        val scored = items.map { it to overlapOf(it) }
        val tiers = scored.groupBy { (_, overlap) -> scorer.band(overlap) }
            .toSortedMap(compareByDescending { it })
        val pickedIds = LinkedHashSet<T>()
        for ((_, rows) in tiers) {
            if (pickedIds.size >= limit) break
            val remaining = limit - pickedIds.size
            val chunk = if (scorer.preferTopic) {
                rows.take(remaining).map { it.first }
            } else {
                takeTier(rows, remaining)
            }
            for (item in chunk) pickedIds.add(item)
        }
        return items.filter { it in pickedIds }
    }

    private fun <T> takeTier(
        rows: List<Pair<T, MeaningRoleOverlap>>,
        remaining: Int,
    ): List<T> {
        if (rows.size <= remaining) return rows.map { it.first }
        val families = LinkedHashMap<Set<String>, MutableList<T>>()
        for ((item, overlap) in rows) {
            families.getOrPut(overlap.family) { mutableListOf() }.add(item)
        }
        if (families.size <= 1) return rows.take(remaining).map { it.first }
        val keys = families.keys.toList()
        val prefix = rows.take(remaining)
        val orderedKeys = keys.sortedWith(
            compareBy<Set<String>> { family ->
                prefix.count { it.second.family == family }
            }.thenBy { family -> keys.indexOf(family) },
        )
        val queues = orderedKeys.associateWith { family ->
            ArrayDeque(families.getValue(family))
        }
        val mixed = ArrayList<T>(remaining)
        val fairShare = (remaining / orderedKeys.size).coerceAtLeast(1)
        for (family in orderedKeys) {
            var taken = 0
            while (taken < fairShare && mixed.size < remaining) {
                val next = queues.getValue(family).removeFirstOrNull() ?: break
                mixed.add(next)
                taken += 1
            }
        }
        while (mixed.size < remaining && queues.any { it.value.isNotEmpty() }) {
            for (family in orderedKeys) {
                if (mixed.size >= remaining) break
                val next = queues.getValue(family).removeFirstOrNull() ?: continue
                mixed.add(next)
            }
        }
        return mixed
    }
}
