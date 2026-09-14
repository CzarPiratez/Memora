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

    private fun hit(id: String, score: Float) = MeaningSearchHit(
        revisionId = MemoryRevisionId(id),
        memoryId = MemoryId("m-$id"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey(id),
        assetType = AssetType.PDF,
        label = id,
        summaryText = "text $id",
        score = score,
        model = model,
    )
}
