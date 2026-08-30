package com.memora.app.application.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciprocalRankFusionTest {
    @Test
    fun fuse_prefers_items_ranked_in_both_lists() {
        val fused = ReciprocalRankFusion.fuse(
            keywordKeys = listOf("a", "b", "c"),
            meaningKeys = listOf("b", "a"),
        )

        assertEquals(3, fused.size)
        val top = fused.take(2).map { it.fusionKey }.toSet()
        assertEquals(setOf("a", "b"), top)
        val bEntry = fused.first { it.fusionKey == "b" }
        assertEquals(2, bEntry.keywordRank)
        assertEquals(1, bEntry.meaningRank)
        assertTrue(bEntry.score > fused.last().score)
    }

    @Test
    fun fuse_includes_single_list_candidates() {
        val fused = ReciprocalRankFusion.fuse(
            keywordKeys = listOf("only-keyword"),
            meaningKeys = emptyList(),
        )

        assertEquals(1, fused.size)
        assertEquals("only-keyword", fused.single().fusionKey)
        assertEquals(1, fused.single().keywordRank)
        assertEquals(null, fused.single().meaningRank)
    }
}
