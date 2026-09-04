package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
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
