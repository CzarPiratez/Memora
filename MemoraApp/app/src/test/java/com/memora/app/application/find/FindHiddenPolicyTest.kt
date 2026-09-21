package com.memora.app.application.find

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindHiddenPolicyTest {
    private val lake = identity("photos", "lake")
    private val map = identity("photos", "map")
    private val note = identity("onenote", "n1")

    @Test
    fun visible_omits_hidden_identities_and_keeps_rank_order() {
        val hits = listOf("lake", "map", "note")
        val identities = mapOf(
            "lake" to lake,
            "map" to map,
            "note" to note,
        )
        val visible = FindHiddenPolicy.visible(
            hits,
            hidden = setOf(map),
            identityOf = { identities.getValue(it) },
        )
        assertEquals(listOf("lake", "note"), visible)
    }

    @Test
    fun ranked_hits_that_are_all_hidden_are_not_treated_as_no_matches() {
        assertTrue(FindHiddenPolicy.rankedHitsAreAllHidden(rankedCount = 2, visibleCount = 0))
        assertFalse(FindHiddenPolicy.rankedHitsAreAllHidden(rankedCount = 0, visibleCount = 0))
        assertFalse(FindHiddenPolicy.rankedHitsAreAllHidden(rankedCount = 2, visibleCount = 1))
    }

    @Test
    fun nothing_hidden_returns_the_same_ranked_hits() {
        val hits = listOf("lake", "map")
        val visible = FindHiddenPolicy.visible(
            hits,
            hidden = emptySet(),
            identityOf = {
                if (it == "lake") lake else map
            },
        )
        assertEquals(hits, visible)
    }

    @Test
    fun restore_list_only_includes_hidden_identities_this_search_ranked() {
        val hidden = listOf("map", "other")
        val identities = mapOf(
            "map" to map,
            "other" to identity("photos", "other"),
        )
        val relevant = FindHiddenPolicy.relevant(
            hidden,
            ranked = setOf(lake, map),
            identityOf = { identities.getValue(it) },
        )
        assertEquals(listOf("map"), relevant)
        assertTrue(
            FindHiddenPolicy.relevant(
                hidden,
                ranked = emptySet(),
                identityOf = { identities.getValue(it) },
            ).isEmpty(),
        )
    }

    private fun identity(source: String, key: String) =
        AssetIdentity(SourceId(source), SourceAssetKey(key))
}
