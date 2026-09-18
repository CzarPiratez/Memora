package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningTrustedHitPolicyTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun keeps_short_band_near_top_score() {
        val hits = listOf(
            hit("a", 0.9f),
            hit("b", 0.85f),
            hit("c", 0.5f),
            hit("d", 0.4f),
        )
        val trusted = MeaningTrustedHitPolicy.apply(hits, limit = 10)
        assertEquals(listOf("a", "b"), trusted.map { it.revisionId.value })
    }

    @Test
    fun respects_max_trusted_cap() {
        val hits = (0 until 8).map { hit("r$it", 0.9f - it * 0.01f) }
        val trusted = MeaningTrustedHitPolicy.apply(hits, limit = 10)
        assertEquals(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS, trusted.size)
    }

    /**
     * D-20 device: Exact files cap at 1.0 after the token boost, so a timetable
     * at 0.55 is outside 0.22. Mixed seating must still keep it.
     */
    @Test
    fun mixed_query_keeps_a_distant_partial_when_exact_hits_fill_the_band() {
        val hits = listOf(
            hit("exact-a", 1.0f, "Swimming schedule term 2"),
            hit("exact-b", 0.98f, "Swimming schedule printout"),
            hit("exact-c", 0.96f, "Swimming schedule screenshot"),
            hit("exact-d", 0.95f, "Swimming schedule heading"),
            hit("exact-e", 0.94f, "Swimming schedule list"),
            hit("timetable", 0.55f, "Grade 2 Swimming timetable 2026 PERIOD TIME"),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 10,
            rawQuery = "swimming schedule",
        )
        assertTrue(trusted.any { it.revisionId.value == "timetable" })
        assertTrue(trusted.any { it.summaryText.contains("schedule") })
        assertEquals(
            MeaningTrustedHitPolicy.MAX_TRUSTED_HITS,
            trusted.size,
        )
    }

    @Test
    fun one_word_query_still_uses_the_score_band() {
        val hits = listOf(
            hit("a", 0.9f, "Passport bio page"),
            hit("b", 0.5f, "Passport renewal notes"),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 10,
            rawQuery = "passport",
        )
        assertEquals(listOf("a"), trusted.map { it.revisionId.value })
    }

    private fun hit(id: String, score: Float, summary: String = "text $id") = MeaningSearchHit(
        revisionId = MemoryRevisionId(id),
        memoryId = MemoryId("m-$id"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey(id),
        assetType = AssetType.PDF,
        label = id,
        summaryText = summary,
        score = score,
        model = model,
    )
}
