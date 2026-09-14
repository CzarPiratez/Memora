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

class MeaningOnlyRecallPolicyTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun a_one_word_miss_is_not_a_paraphrase() {
        val kept = MeaningOnlyRecallPolicy.select(
            hits = listOf(hit("fashion", 0.9f)),
            namedWordCount = 1,
        )
        assertTrue(kept.isEmpty())
    }

    @Test
    fun a_weak_cosine_neighbour_stays_empty() {
        val kept = MeaningOnlyRecallPolicy.select(
            hits = listOf(hit("bus", 0.18f)),
            namedWordCount = 2,
        )
        assertTrue(kept.isEmpty())
    }

    @Test
    fun a_strong_cosine_band_is_admitted_and_capped() {
        val kept = MeaningOnlyRecallPolicy.select(
            hits = listOf(
                hit("swim", 0.62f),
                hit("lessons", 0.55f),
                hit("nearby", 0.54f),
                hit("far", 0.33f),
                hit("noise", 0.20f),
            ),
            namedWordCount = 3,
        )
        assertEquals(listOf("swim", "lessons", "nearby"), kept.map { it.revisionId.value })
    }

    /**
     * The word assist must not buy a seat on this tier. A hit can arrive here
     * boosted by a word the precision gate never required — a TIME word such as
     * `recent` — and `0.18 + 0.35` clears a floor that `0.18` never earned.
     */
    @Test
    fun a_boosted_score_cannot_clear_the_floor_for_a_weak_neighbour() {
        val kept = MeaningOnlyRecallPolicy.select(
            hits = listOf(hit("bus", score = 0.53f, cosine = 0.18f, boosted = true)),
            namedWordCount = 2,
        )
        assertTrue(kept.isEmpty())
    }

    /**
     * On this tier no named word is in the file, so an admitted hit must not
     * keep telling Why that "a word you typed" helped find it.
     */
    @Test
    fun admission_strips_the_word_assist_from_rank_and_from_why() {
        val kept = MeaningOnlyRecallPolicy.select(
            hits = listOf(hit("timetable", score = 0.93f, cosine = 0.58f, boosted = true)),
            namedWordCount = 3,
        )
        assertEquals(1, kept.size)
        assertEquals(0.58f, kept.single().score, 0.0001f)
        assertFalse(kept.single().evidenceTokenBoosted)
    }

    /** The band is measured on similarity, so a boost cannot widen it either. */
    @Test
    fun the_band_gap_is_measured_on_cosine_not_on_boosted_score() {
        val kept = MeaningOnlyRecallPolicy.select(
            hits = listOf(
                hit("top", score = 0.60f, cosine = 0.60f),
                hit("distant", score = 0.75f, cosine = 0.40f, boosted = true),
            ),
            namedWordCount = 2,
        )
        assertEquals(listOf("top"), kept.map { it.revisionId.value })
    }

    private fun hit(
        id: String,
        score: Float,
        cosine: Float = score,
        boosted: Boolean = false,
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId(id),
        memoryId = MemoryId("m-$id"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey(id),
        assetType = AssetType.PDF,
        label = id,
        summaryText = "text $id",
        score = score,
        model = model,
        evidenceTokenBoosted = boosted,
        cosine = cosine,
    )
}
