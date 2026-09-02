package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityRecallRankerTest {
    @Test
    fun rank_ordersByScoreDescending() {
        val result = IdentityRecallRanker.rank(
            query = "invoice",
            candidates = listOf(
                RecallRankCandidate(id = "a", score = 0.2f),
                RecallRankCandidate(id = "c", score = 0.9f),
                RecallRankCandidate(id = "b", score = 0.5f),
            ),
        )
        assertTrue(result is RecallRankResult.Ranked)
        assertEquals(listOf("c", "b", "a"), (result as RecallRankResult.Ranked).orderedIds)
    }

    @Test
    fun unavailableRanker_returnsUnavailable() {
        val result = UnavailableLocalIntelligence.recallRanker("no pack").rank(
            query = "test",
            candidates = listOf(RecallRankCandidate(id = "x", score = 1f)),
        )
        assertTrue(result is RecallRankResult.Unavailable)
    }
}
