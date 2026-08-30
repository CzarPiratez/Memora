package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryAnchor
import com.memora.app.domain.memory.MemoryAnchorId
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.MemoryText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorAwareMeaningRecallRankingTest {
    private val model = ModelVersionIdentity("test-model", "1")

    @Test
    fun apply_explicit_time_excludes_non_matching_anchor_hits() = runBlocking {
        val revMatch = MemoryRevisionId("rev-match")
        val revMiss = MemoryRevisionId("rev-miss")
        val outcome = MeaningSearchOutcome.Matches(
            query = "notes in 2024",
            hits = listOf(
                hit(revMiss, 0.95f),
                hit(revMatch, 0.9f),
            ),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "notes in 2024",
            memoryRepository = FakeAnchorRepository(
                mapOf(
                    revMatch to listOf(timeAnchor("Date taken: 2024-03-01")),
                    revMiss to listOf(timeAnchor("Date taken: 2022-01-01")),
                ),
            ),
        ) as MeaningSearchOutcome.Matches

        assertEquals(1, filtered.hits.size)
        assertEquals(revMatch, filtered.hits.single().revisionId)
    }

    @Test
    fun apply_token_boost_outranks_higher_cosine_without_matching_token() = runBlocking {
        val foxtrot = MemoryRevisionId("rev-5")
        val miraPage = MemoryRevisionId("rev-3")
        val outcome = MeaningSearchOutcome.Matches(
            query = "mira",
            hits = listOf(
                hit(
                    revisionId = foxtrot,
                    score = 0.4f,
                    label = "memora-open-5page.pdf",
                    summaryText = "Page 1 FOXTROT cover sheet",
                    rankedPdfPageNumber = 1,
                ),
                hit(
                    revisionId = miraPage,
                    score = 0.2f,
                    label = "memora-open-3page.pdf",
                    summaryText = "Page 3 ECHO meet mira follow-up",
                    rankedPdfPageNumber = 3,
                ),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "mira",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        val top = ranked.hits.first()
        assertEquals("memora-open-3page.pdf", top.label)
        assertEquals(3, top.rankedPdfPageNumber)
        assertTrue(top.evidenceTokenBoosted)
        assertTrue(top.score > ranked.hits[1].score)
    }

    @Test
    fun apply_without_time_or_topic_cue_returns_boost_only() = runBlocking {
        val outcome = MeaningSearchOutcome.Matches(
            query = "wifi",
            hits = listOf(hit(MemoryRevisionId("rev-1"), 0.5f)),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "wifi",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(outcome.hits.single().revisionId, filtered.hits.single().revisionId)
        assertEquals(outcome.hits.single().score, filtered.hits.single().score, 0f)
        assertFalse(filtered.hits.single().evidenceTokenBoosted)
    }

    private fun hit(
        revisionId: MemoryRevisionId,
        score: Float,
        label: String = "Label",
        summaryText: String = "Summary",
        rankedPdfPageNumber: Int? = null,
    ) = MeaningSearchHit(
        revisionId = revisionId,
        memoryId = MemoryId("mem-${revisionId.value}"),
        sourceId = SourceId("src-1"),
        sourceAssetKey = SourceAssetKey(label),
        assetType = AssetType.PDF,
        label = label,
        summaryText = summaryText,
        score = score,
        model = model,
        rankedPdfPageNumber = rankedPdfPageNumber,
    )

    private fun hit(revisionId: MemoryRevisionId, score: Float) = hit(
        revisionId = revisionId,
        score = score,
        label = "Label",
        summaryText = "Summary",
    )

    private fun timeAnchor(text: String) = MemoryAnchor(
        id = MemoryAnchorId("time-1"),
        kind = MemoryAnchorKind.TIME,
        text = MemoryText(text),
        evidenceIds = setOf(MemoryEvidenceId("e-time")),
    )

    private class FakeAnchorRepository(
        private val anchors: Map<MemoryRevisionId, List<MemoryAnchor>> = emptyMap(),
    ) : MemoryRepository by EmptyMemoryRepositoryDelegate() {
        override suspend fun findSignatureAnchors(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, List<MemoryAnchor>> =
            anchors.filterKeys { it in revisionIds }
    }
}
