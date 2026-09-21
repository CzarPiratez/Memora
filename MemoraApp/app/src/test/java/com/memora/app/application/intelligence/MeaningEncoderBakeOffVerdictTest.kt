package com.memora.app.application.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningEncoderBakeOffVerdictTest {
    @Test
    fun frozen_use_card_matches_founder_probe() {
        val card = MeaningEncoderUseBaseline.card
        assertEquals(17, card.cues)
        assertEquals(12, card.goldMatched)
        assertEquals(0, card.goldAt1)
        assertEquals(0, card.goldAt10)
        assertEquals(0, card.goldAt60)
        assertEquals(352, card.medianAssetRank)
        assertEquals(3046, MeaningEncoderUseBaseline.VECTORS_SCANNED)
        assertEquals(MeaningEncoderUseBaseline.card, MeaningEncoderBakeOffScorecard.ofRanks(
            packId = MeaningEncoderBakeOffScorecard.CURRENT_PACK_ID,
            matchedAssetRanks = MeaningEncoderUseBaseline.matchedAssetRanks,
            unmatched = MeaningEncoderUseBaseline.UNMATCHED,
        ))
    }

    @Test
    fun live_use_probe_awaits_challenger() {
        val live = MeaningEncoderUseBaseline.card
        val verdict = MeaningEncoderBakeOffVerdict.decide(live = live)
        assertTrue(verdict is MeaningEncoderBakeOffVerdict.AwaitingChallenger)
        assertEquals(0, (verdict as MeaningEncoderBakeOffVerdict.AwaitingChallenger).liveGoldAt60)
        assertTrue(verdict.logLine().contains("median_only=false"))
    }

    @Test
    fun gold_at_60_lift_wins_without_query_text() {
        val challenger = MeaningEncoderBakeOffScorecard.ofRanks(
            packId = "retriever-small",
            matchedAssetRanks = listOf(12) + List(11) { 80 },
            unmatched = 5,
        )
        val verdict = MeaningEncoderBakeOffVerdict.decide(
            live = MeaningEncoderUseBaseline.card,
            challenger = challenger,
        )
        assertTrue(verdict is MeaningEncoderBakeOffVerdict.ChallengerWins)
        assertEquals(1, (verdict as MeaningEncoderBakeOffVerdict.ChallengerWins).goldAt60)
    }

    @Test
    fun better_median_still_outside_the_pool_does_not_win() {
        val closer = MeaningEncoderUseBaseline.matchedAssetRanks.map { rank ->
            (rank / 2).coerceAtLeast(61)
        }
        val challenger = MeaningEncoderBakeOffScorecard.ofRanks(
            packId = "retriever-small",
            matchedAssetRanks = closer,
            unmatched = 5,
        )
        assertTrue(challenger.medianAssetRank!! < MeaningEncoderUseBaseline.card.medianAssetRank!!)
        assertEquals(0, challenger.goldAt60)
        val verdict = MeaningEncoderBakeOffVerdict.decide(
            live = MeaningEncoderUseBaseline.card,
            challenger = challenger,
        )
        assertTrue(verdict is MeaningEncoderBakeOffVerdict.ChallengerDoesNotWin)
        assertTrue(
            (verdict as MeaningEncoderBakeOffVerdict.ChallengerDoesNotWin).reason.contains("gold@60"),
        )
    }

    @Test
    fun fewer_cues_are_incomparable() {
        val challenger = MeaningEncoderBakeOffScorecard.ofRanks(
            packId = "retriever-small",
            matchedAssetRanks = listOf(1),
            unmatched = 0,
        )
        val verdict = MeaningEncoderBakeOffVerdict.decide(
            live = MeaningEncoderUseBaseline.card,
            challenger = challenger,
        )
        assertTrue(verdict is MeaningEncoderBakeOffVerdict.Incomparable)
    }

    @Test
    fun dropping_matched_gold_does_not_win() {
        val challenger = MeaningEncoderBakeOffScorecard.ofRanks(
            packId = "retriever-small",
            matchedAssetRanks = listOf(1, 2, 3),
            unmatched = 14,
        )
        val verdict = MeaningEncoderBakeOffVerdict.decide(
            live = MeaningEncoderUseBaseline.card,
            challenger = challenger,
        )
        assertTrue(verdict is MeaningEncoderBakeOffVerdict.ChallengerDoesNotWin)
        assertTrue(
            (verdict as MeaningEncoderBakeOffVerdict.ChallengerDoesNotWin).reason.contains("matched"),
        )
    }

    @Test
    fun win_bar_has_no_specimen_wording() {
        val source = listOf(
            MeaningEncoderUseBaseline.card.logLine(),
            MeaningEncoderBakeOffVerdict.decide(live = MeaningEncoderUseBaseline.card).logLine(),
            MeaningEncoderBakeOffVerdict.decide(live = MeaningEncoderUseBaseline.card).displayLine(),
        ).joinToString(" ").lowercase()
        assertFalse(source.contains("swimming"))
        assertFalse(source.contains("passport"))
        assertFalse(source.contains("timetable"))
        assertFalse(source.contains("grade"))
    }
}
