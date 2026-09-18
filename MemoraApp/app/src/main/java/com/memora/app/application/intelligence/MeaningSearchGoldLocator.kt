package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.MeaningRecallCue

/**
 * Live-path gold membership for D-22. Same needles and label/key match as the
 * encoder probe. Counts and a short filename only — no excerpts.
 *
 * Does not change ranking or pool seating.
 */
object MeaningSearchGoldLocator {
    const val DIAGNOSIS_NO_GOLD_CUE = "no_gold_cue"
    const val DIAGNOSIS_NOT_IN_COLLAPSE = "not_in_collapse"
    const val DIAGNOSIS_OUT_OF_POOL = "out_of_pool"
    const val DIAGNOSIS_DROPPED_BY_TIER = "dropped_by_tier"
    const val DIAGNOSIS_IN_POOL_OFF_PAGE = "in_pool_off_page"
    const val DIAGNOSIS_ON_PAGE = "on_page"

    fun cueFor(rawQuery: String): MeaningEncoderProbeCue? {
        val display = MeaningRecallCue.displayQuery(rawQuery)
        if (display.isEmpty()) return null
        return MeaningEncoderProbeCues.DEFAULT.firstOrNull {
            MeaningRecallCue.displayQuery(it.query).equals(display, ignoreCase = true)
        }
    }

    fun locate(
        rawQuery: String,
        collapseHits: List<MeaningSearchHit>,
        admittedHits: List<MeaningSearchHit>,
    ): MeaningSearchGoldMembership {
        val cue = cueFor(rawQuery) ?: return MeaningSearchGoldMembership(registered = false)
        val needles = cue.goldSubstrings.map { it.lowercase() }
        val collapseMatch = collapseHits.withIndex().firstOrNull { (_, hit) ->
            matchesGold(hit, needles)
        }
        if (collapseMatch == null) {
            return MeaningSearchGoldMembership(registered = true)
        }
        val (collapseIndex, hit) = collapseMatch
        val identity = assetIdentity(hit)
        val admittedRank = rankOf(identity, admittedHits)
        return MeaningSearchGoldMembership(
            registered = true,
            goldLabel = MeaningSearchTrace.shortLabel(hit.label),
            goldAssetType = hit.assetType.name,
            goldAssetIdentity = identity,
            collapseRank = collapseIndex + 1,
            collapseCosine = hit.cosine,
            admittedRank = admittedRank,
        )
    }

    fun rankOf(identity: String?, hits: List<MeaningSearchHit>): Int? {
        if (identity.isNullOrBlank()) return null
        return hits.indexOfFirst { assetIdentity(it) == identity }
            .takeIf { it >= 0 }
            ?.plus(1)
    }

    fun diagnosis(
        membership: MeaningSearchGoldMembership,
        rankedRank: Int?,
        shownRank: Int?,
    ): String {
        if (!membership.registered) return DIAGNOSIS_NO_GOLD_CUE
        if (membership.collapseRank == null) return DIAGNOSIS_NOT_IN_COLLAPSE
        if (membership.admittedRank == null) return DIAGNOSIS_OUT_OF_POOL
        if (rankedRank == null) return DIAGNOSIS_DROPPED_BY_TIER
        if (shownRank == null) return DIAGNOSIS_IN_POOL_OFF_PAGE
        return DIAGNOSIS_ON_PAGE
    }

    fun matchesGold(hit: MeaningSearchHit, needles: List<String>): Boolean {
        val labelLower = hit.label.lowercase()
        val keyLower = hit.sourceAssetKey.value.lowercase()
        return needles.any { needle ->
            labelLower.contains(needle) || keyLower.contains(needle)
        }
    }

    fun assetIdentity(hit: MeaningSearchHit): String =
        "${hit.sourceId.value}|${hit.sourceAssetKey.value}"
}

/**
 * Best gold asset after collapse. Identity is for matching later stages only
 * and is never written to Logcat.
 */
data class MeaningSearchGoldMembership(
    val registered: Boolean,
    val goldLabel: String? = null,
    val goldAssetType: String? = null,
    val goldAssetIdentity: String? = null,
    val collapseRank: Int? = null,
    val collapseCosine: Float? = null,
    val admittedRank: Int? = null,
)
