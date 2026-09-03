package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.IdentityRecallRanker
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.RecallRankCandidate
import com.memora.app.domain.intelligence.RecallRankResult
import com.memora.app.domain.intelligence.RecallRanker
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AnchorAwareMeaningRecallRankingStageATest {
    private val model = ModelVersionIdentity("test-model", "1")

    @Test
    fun apply_reranks_after_lexical_using_recall_ranker_order() = runBlocking {
        val low = MemoryRevisionId("rev-low")
        val high = MemoryRevisionId("rev-high")
        val outcome = MeaningSearchOutcome.Matches(
            query = "invoice",
            hits = listOf(
                hit(low, 0.9f, "old invoice summary"),
                hit(high, 0.2f, "GOLF invoice number"),
            ),
            limitReached = false,
            model = model,
        )
        val ranker = object : RecallRanker {
            override fun availability(): CapabilityAvailability =
                CapabilityAvailability.Available(ModelVersionIdentity("fake-ce", "1"))

            override fun limits(): CapabilityLimits? = null

            override fun rank(
                query: String,
                candidates: List<RecallRankCandidate>,
            ): RecallRankResult = RecallRankResult.Ranked(
                listOf(high.value, low.value),
            )
        }

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "invoice",
            memoryRepository = EmptyMemoryRepositoryDelegate(),
            recallRanker = ranker,
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(high, low), ranked.hits.map { it.revisionId })
    }

    @Test
    fun apply_keeps_score_order_with_identity_ranker() = runBlocking {
        val a = MemoryRevisionId("rev-a")
        val b = MemoryRevisionId("rev-b")
        val outcome = MeaningSearchOutcome.Matches(
            query = "notes",
            hits = listOf(hit(a, 0.9f, "alpha notes"), hit(b, 0.8f, "beta notes")),
            limitReached = false,
            model = model,
        )
        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "notes",
            memoryRepository = EmptyMemoryRepositoryDelegate(),
            recallRanker = IdentityRecallRanker,
        ) as MeaningSearchOutcome.Matches
        assertEquals(listOf(a, b), ranked.hits.map { it.revisionId })
    }

    private fun hit(
        revisionId: MemoryRevisionId,
        score: Float,
        summary: String,
    ) = MeaningSearchHit(
        revisionId = revisionId,
        memoryId = MemoryId("mem-${revisionId.value}"),
        sourceId = SourceId("src"),
        sourceAssetKey = SourceAssetKey("asset"),
        assetType = AssetType.PDF,
        label = summary,
        summaryText = summary,
        score = score,
        model = model,
    )
}
