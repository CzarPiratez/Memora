package com.memora.app.application.memory

import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId

/**
 * Reciprocal Rank Fusion (FC-01) for keyword + meaning candidate lists inside
 * [CanonicalRecall]. Deterministic; no parallel Find path.
 */
object ReciprocalRankFusion {
    const val DEFAULT_K = 60

    data class RankedEntry(
        val fusionKey: String,
        val score: Float,
        val keywordRank: Int?,
        val meaningRank: Int?,
    )

    fun fuse(
        keywordKeys: List<String>,
        meaningKeys: List<String>,
        k: Int = DEFAULT_K,
    ): List<RankedEntry> {
        require(k > 0)
        val scores = linkedMapOf<String, Float>()
        val keywordRankByKey = linkedMapOf<String, Int>()
        val meaningRankByKey = linkedMapOf<String, Int>()

        keywordKeys.forEachIndexed { index, key ->
            val rank = index + 1
            keywordRankByKey[key] = rank
            scores[key] = (scores[key] ?: 0f) + contribution(rank, k)
        }
        meaningKeys.forEachIndexed { index, key ->
            val rank = index + 1
            meaningRankByKey[key] = rank
            scores[key] = (scores[key] ?: 0f) + contribution(rank, k)
        }

        return scores.entries
            .sortedByDescending { it.value }
            .map { (key, score) ->
                RankedEntry(
                    fusionKey = key,
                    score = score,
                    keywordRank = keywordRankByKey[key],
                    meaningRank = meaningRankByKey[key],
                )
            }
    }

    fun fusionKey(sourceId: SourceId, sourceAssetKey: SourceAssetKey): String =
        "${sourceId.value}\u001f${sourceAssetKey.value}"

    private fun contribution(rank: Int, k: Int): Float = 1f / (k + rank)
}
