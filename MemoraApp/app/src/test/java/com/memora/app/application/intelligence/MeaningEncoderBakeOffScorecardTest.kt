package com.memora.app.application.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningEncoderBakeOffScorecardTest {
    @Test
    fun counts_gold_bands_and_median_without_excerpts() {
        val rows = listOf(
            row("a", rank = 1),
            row("b", rank = 4),
            row("c", rank = 40),
            row("d", rank = null),
            row("e", rank = 2, skipped = "no content tokens"),
        )
        val card = MeaningEncoderBakeOffScorecard.of(rows)
        assertEquals(4, card.cues)
        assertEquals(3, card.goldMatched)
        assertEquals(1, card.goldAt1)
        assertEquals(2, card.goldAt10)
        assertEquals(3, card.goldAt60)
        assertEquals(4, card.medianAssetRank)
        assertEquals(MeaningEncoderBakeOffScorecard.CHALLENGER_NOT_INSTALLED, card.challenger)
        assertTrue(card.logLine().contains("gold@10=2"))
        assertTrue(card.displayLine().contains("mediapipe-use"))
    }

    @Test
    fun founder_ranks_reproduce_the_frozen_card() {
        val rows = MeaningEncoderUseBaseline.matchedAssetRanks.mapIndexed { index, rank ->
            row("q$index", rank)
        } + List(MeaningEncoderUseBaseline.UNMATCHED) { index ->
            row("u$index", rank = null)
        }
        assertEquals(MeaningEncoderUseBaseline.card, MeaningEncoderBakeOffScorecard.of(rows))
    }

    @Test
    fun empty_probe_is_a_zero_card() {
        val card = MeaningEncoderBakeOffScorecard.of(emptyList())
        assertEquals(0, card.cues)
        assertEquals(0, card.goldMatched)
        assertEquals(null, card.medianAssetRank)
    }

    private fun row(
        query: String,
        rank: Int?,
        skipped: String? = null,
    ) = MeaningEncoderProbeRow(
        query = query,
        embedText = query,
        contentTokens = listOf("token"),
        vectorsScanned = 10,
        assetRankAfterCollapse = rank,
        skipped = skipped,
    )
}
