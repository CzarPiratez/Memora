package com.memora.app.application.intelligence

/**
 * One-pack scorecard for ADR-055 slice 3. Does not swap the product
 * embedder. A challenger wins later only if it beats this card on the
 * same cue set without a new occupancy table.
 *
 * Ranks are collapsed **asset** ranks from the read-only encoder probe
 * (no token boost, no Find trim).
 */
data class MeaningEncoderBakeOffScorecard(
    val packId: String,
    val cues: Int,
    val goldMatched: Int,
    val goldAt1: Int,
    val goldAt10: Int,
    val goldAt60: Int,
    val medianAssetRank: Int?,
    val challenger: String = CHALLENGER_NOT_INSTALLED,
) {
    init {
        require(packId.isNotBlank())
        require(cues >= 0)
        require(goldMatched in 0..cues)
        require(goldAt1 in 0..goldMatched)
        require(goldAt10 in goldAt1..goldMatched)
        require(goldAt60 in goldAt10..goldMatched)
        require(medianAssetRank == null || medianAssetRank > 0)
        require(challenger.isNotBlank())
    }

    fun logLine(): String =
        "bakeoff pack=$packId cues=$cues matched=$goldMatched " +
            "gold@1=$goldAt1 gold@10=$goldAt10 gold@60=$goldAt60 " +
            "medianAssetRank=${medianAssetRank ?: "-"} challenger=$challenger"

    fun displayLine(): String =
        "$packId: gold@1=$goldAt1 gold@10=$goldAt10 gold@60=$goldAt60 " +
            "median=${medianAssetRank ?: "—"} of $goldMatched matched / $cues cues. " +
            "Challenger $challenger."

    companion object {
        const val CURRENT_PACK_ID = "mediapipe-use"
        const val CHALLENGER_NOT_INSTALLED = "not_installed"

        fun of(
            rows: List<MeaningEncoderProbeRow>,
            packId: String = CURRENT_PACK_ID,
            challenger: String = CHALLENGER_NOT_INSTALLED,
        ): MeaningEncoderBakeOffScorecard {
            val scored = rows.filter { it.skipped == null }
            val ranks = scored.mapNotNull { it.assetRankAfterCollapse }
            val unmatched = scored.size - ranks.size
            return ofRanks(
                packId = packId,
                matchedAssetRanks = ranks,
                unmatched = unmatched,
                challenger = challenger,
            )
        }

        fun ofRanks(
            packId: String,
            matchedAssetRanks: List<Int>,
            unmatched: Int,
            challenger: String = CHALLENGER_NOT_INSTALLED,
        ): MeaningEncoderBakeOffScorecard {
            require(unmatched >= 0)
            require(matchedAssetRanks.all { it > 0 })
            return MeaningEncoderBakeOffScorecard(
                packId = packId,
                cues = matchedAssetRanks.size + unmatched,
                goldMatched = matchedAssetRanks.size,
                goldAt1 = matchedAssetRanks.count { it == 1 },
                goldAt10 = matchedAssetRanks.count { it <= 10 },
                goldAt60 = matchedAssetRanks.count { it <= 60 },
                medianAssetRank = median(matchedAssetRanks),
                challenger = challenger,
            )
        }

        private fun median(values: List<Int>): Int? {
            if (values.isEmpty()) return null
            val sorted = values.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) {
                sorted[mid]
            } else {
                (sorted[mid - 1] + sorted[mid]) / 2
            }
        }
    }
}
