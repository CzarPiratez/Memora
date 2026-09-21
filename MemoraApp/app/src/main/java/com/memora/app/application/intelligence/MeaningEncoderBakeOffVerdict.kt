package com.memora.app.application.intelligence

/**
 * Slice 3 / 4 swap gate. Encoder-only. Vocabulary-free.
 *
 * The product meaning pool is 60. USE gold@60 is 0, so a closer median
 * that still never enters that pool is not a win. gold@1 / gold@10 must
 * not regress. Cue wording is not a factor.
 *
 * Does not swap the product embedder. Does not change Find ranking.
 */
sealed class MeaningEncoderBakeOffVerdict {
    abstract fun logLine(): String

    abstract fun displayLine(): String

    data class AwaitingChallenger(
        val liveGoldAt60: Int,
    ) : MeaningEncoderBakeOffVerdict() {
        override fun logLine(): String =
            "verdict=awaiting_challenger liveGold@60=$liveGoldAt60 " +
                "bar=gold@60_strictly_greater median_only=false"

        override fun displayLine(): String =
            "Swap bar: a later pack must beat gold@60=${MeaningEncoderUseBaseline.card.goldAt60}. " +
                "A better median still outside the pool is not a win."
    }

    data class Incomparable(
        val reason: String,
    ) : MeaningEncoderBakeOffVerdict() {
        override fun logLine(): String = "verdict=incomparable reason=$reason"

        override fun displayLine(): String = "Bake-off incomparable ($reason)."
    }

    data class ChallengerWins(
        val goldAt60: Int,
        val useGoldAt60: Int,
    ) : MeaningEncoderBakeOffVerdict() {
        override fun logLine(): String =
            "verdict=challenger_wins gold@60=$goldAt60 useGold@60=$useGoldAt60"

        override fun displayLine(): String =
            "Challenger wins gold@60=$goldAt60 vs USE $useGoldAt60. Swap is still slice 4."
    }

    data class ChallengerDoesNotWin(
        val reason: String,
    ) : MeaningEncoderBakeOffVerdict() {
        override fun logLine(): String = "verdict=challenger_does_not_win reason=$reason"

        override fun displayLine(): String = "Challenger does not win ($reason)."
    }

    companion object {
        fun decide(
            use: MeaningEncoderBakeOffScorecard = MeaningEncoderUseBaseline.card,
            live: MeaningEncoderBakeOffScorecard,
            challenger: MeaningEncoderBakeOffScorecard? = null,
        ): MeaningEncoderBakeOffVerdict {
            val other = challenger?.takeUnless {
                it.packId == MeaningEncoderBakeOffScorecard.CHALLENGER_NOT_INSTALLED ||
                    it.packId == use.packId
            }
            if (other == null) {
                return AwaitingChallenger(liveGoldAt60 = live.goldAt60)
            }
            if (other.cues != use.cues) {
                return Incomparable("cue count ${other.cues} != ${use.cues}")
            }
            if (other.goldMatched < use.goldMatched) {
                return ChallengerDoesNotWin(
                    "matched ${other.goldMatched} < ${use.goldMatched}",
                )
            }
            if (other.goldAt60 <= use.goldAt60) {
                return ChallengerDoesNotWin(
                    "gold@60 ${other.goldAt60} <= ${use.goldAt60}",
                )
            }
            if (other.goldAt10 < use.goldAt10) {
                return ChallengerDoesNotWin(
                    "gold@10 ${other.goldAt10} < ${use.goldAt10}",
                )
            }
            if (other.goldAt1 < use.goldAt1) {
                return ChallengerDoesNotWin(
                    "gold@1 ${other.goldAt1} < ${use.goldAt1}",
                )
            }
            return ChallengerWins(
                goldAt60 = other.goldAt60,
                useGoldAt60 = use.goldAt60,
            )
        }
    }
}
