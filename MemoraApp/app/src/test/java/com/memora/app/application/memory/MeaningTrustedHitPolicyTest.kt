package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningTrustedHitPolicyTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun keeps_short_band_near_top_cosine() {
        val hits = listOf(
            hit("a", score = 0.9f, cosine = 0.9f),
            hit("b", score = 0.85f, cosine = 0.85f),
            hit("c", score = 0.5f, cosine = 0.5f),
            hit("d", score = 0.4f, cosine = 0.4f),
        )
        val trusted = MeaningTrustedHitPolicy.apply(hits, limit = 20)
        assertEquals(listOf("a", "b"), trusted.map { it.revisionId.value })
    }

    @Test
    fun respects_max_trusted_cap() {
        val hits = (0 until 25).map { hit("r$it", score = 0.9f, cosine = 0.9f - it * 0.001f) }
        val trusted = MeaningTrustedHitPolicy.apply(hits, limit = 40)
        assertEquals(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS, trusted.size)
    }

    @Test
    fun boosted_score_does_not_set_the_band() {
        val hits = listOf(
            hit("boosted-exact", score = 1.0f, cosine = 0.62f, summary = "Swimming schedule"),
            hit("neighbour", score = 0.55f, cosine = 0.50f, summary = "Swimming timetable"),
        )
        val trusted = MeaningTrustedHitPolicy.apply(hits, limit = 20)
        assertEquals(listOf("boosted-exact", "neighbour"), trusted.map { it.revisionId.value })
    }

    @Test
    fun query_and_asset_type_do_not_reorder_or_reserve_seats() {
        val hits = listOf(
            hit("shot", 0.80f, "Swimming timetable photo", AssetType.SCREENSHOT, cosine = 0.80f),
            hit("pdf", 0.79f, "Swimming timetable PDF", AssetType.PDF, cosine = 0.79f),
            hit("note", 0.78f, "Swimming timetable note", AssetType.NOTE, cosine = 0.78f),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 20,
            rawQuery = "swimming timetable",
        )
        assertEquals(listOf("shot", "pdf", "note"), trusted.map { it.revisionId.value })
    }

    @Test
    fun a_far_cosine_neighbour_still_drops() {
        val hits = listOf(
            hit("a", score = 0.9f, cosine = 0.9f),
            hit("b", score = 0.5f, cosine = 0.5f),
        )
        val trusted = MeaningTrustedHitPolicy.apply(hits, limit = 20, rawQuery = "passport")
        assertEquals(listOf("a"), trusted.map { it.revisionId.value })
    }

    @Test
    fun does_not_pad_a_thin_band_to_the_cap() {
        val hits = listOf(
            hit("a", score = 0.91f, cosine = 0.91f),
            hit("b", score = 0.90f, cosine = 0.90f),
            hit("c", score = 0.89f, cosine = 0.89f),
        )
        val page = MeaningTrustedHitPolicy.page(hits, limit = 20)
        assertEquals(3, page.hits.size)
        assertFalse(page.truncatedByPageCap)
    }

    @Test
    fun truncated_only_when_the_band_exceeds_the_cap() {
        val inBand = (0 until 25).map { hit("r$it", score = 0.9f, cosine = 0.9f - it * 0.001f) }
        val page = MeaningTrustedHitPolicy.page(inBand, limit = 20)
        assertEquals(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS, page.hits.size)
        assertTrue(page.truncatedByPageCap)
    }

    @Test
    fun mixed_named_word_depths_share_a_capped_page() {
        val twoToken = (0 until 20).map { index ->
            hit(
                id = "class-$index",
                score = 0.84f - index * 0.001f,
                summary = "swimming classes photo $index",
                cosine = 0.84f - index * 0.001f,
            )
        }
        val timetable = hit(
            id = "tt",
            score = 0.675f,
            summary = "weekly swimming timetable",
            cosine = 0.675f,
        )
        val page = MeaningTrustedHitPolicy.page(
            hits = twoToken + timetable,
            limit = 20,
            rawQuery = "when are the swimming classes",
        )
        assertEquals(20, page.hits.size)
        assertTrue(page.truncatedByPageCap)
        assertTrue(page.hits.any { it.revisionId.value == "tt" })
        assertEquals("class-0", page.hits.first().revisionId.value)
        assertEquals("tt", page.hits[1].revisionId.value)
    }

    @Test
    fun same_depth_named_word_families_share_a_capped_page() {
        val classesOnly = (0 until 20).map { index ->
            hit(
                id = "classes-$index",
                score = 0.84f - index * 0.001f,
                summary = "beginner classes list $index",
                cosine = 0.84f - index * 0.001f,
            )
        }
        val timetable = hit(
            id = "tt",
            score = 0.675f,
            summary = "weekly swimming timetable",
            cosine = 0.675f,
        )
        val page = MeaningTrustedHitPolicy.page(
            hits = classesOnly + timetable,
            limit = 20,
            rawQuery = "when are the swimming classes",
        )
        assertEquals(20, page.hits.size)
        assertTrue(page.truncatedByPageCap)
        assertTrue(page.hits.any { it.revisionId.value == "tt" })
        assertEquals("tt", page.hits.first().revisionId.value)
        assertEquals("classes-0", page.hits[1].revisionId.value)
    }

    @Test
    fun one_word_cue_does_not_reorder_a_capped_page() {
        val hits = (0 until 25).map { index ->
            hit(
                id = "p$index",
                score = 0.9f - index * 0.001f,
                summary = "passport copy $index",
                cosine = 0.9f - index * 0.001f,
            )
        }
        val page = MeaningTrustedHitPolicy.page(hits, limit = 20, rawQuery = "passport")
        assertEquals((0 until 20).map { "p$it" }, page.hits.map { it.revisionId.value })
    }

    private fun hit(
        id: String,
        score: Float,
        summary: String = "text $id",
        assetType: AssetType = AssetType.PDF,
        cosine: Float = score,
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId(id),
        memoryId = MemoryId("m-$id"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey(id),
        assetType = assetType,
        label = id,
        summaryText = summary,
        score = score,
        model = model,
        cosine = cosine,
    )
}
