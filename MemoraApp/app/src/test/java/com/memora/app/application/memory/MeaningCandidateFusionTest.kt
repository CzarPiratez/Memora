package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningCandidateFusionTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun a_lexical_only_asset_is_appended_without_changing_measured_cosine() {
        val meaningHit = meaningHit("near", cosine = 0.81f)
        val lexicalGold = lexicalHit(
            id = "gold",
            label = "Grade-2-Swimming-TT-2026.pdf",
            excerpt = "grade 2 weekly swimming timetable",
        )
        val fused = MeaningCandidateFusion.merge(
            meaning = MeaningSearchOutcome.Matches(
                query = "when are the swimming classes for grade 2",
                hits = listOf(meaningHit),
                limitReached = false,
                model = model,
            ),
            lexicalHits = listOf(lexicalGold),
        )
        assertEquals(listOf("near", "gold"), fused.hits.map { it.revisionId.value })
        assertEquals(0.81f, fused.hits.first().cosine)
        assertEquals(MeaningCandidateFusion.LEXICAL_ONLY_COSINE, fused.hits.last().cosine)
    }

    @Test
    fun an_asset_already_in_the_meaning_pool_is_not_duplicated() {
        val meaningHit = meaningHit("gold", cosine = 0.44f, label = "TT.pdf")
        val lexicalSame = lexicalHit(id = "gold", label = "TT.pdf", excerpt = "swimming timetable")
        val fused = MeaningCandidateFusion.merge(
            meaning = MeaningSearchOutcome.Matches(
                query = "swimming",
                hits = listOf(meaningHit),
                limitReached = false,
                model = model,
            ),
            lexicalHits = listOf(lexicalSame),
        )
        assertEquals(1, fused.hits.size)
        assertEquals(0.44f, fused.hits.single().cosine)
    }

    @Test
    fun empty_lexical_leaves_the_meaning_list_unchanged() {
        val meaningHit = meaningHit("a", cosine = 0.7f)
        val outcome = MeaningSearchOutcome.Matches(
            query = "passport",
            hits = listOf(meaningHit),
            limitReached = false,
            model = model,
        )
        assertTrue(MeaningCandidateFusion.merge(outcome, emptyList()) === outcome)
    }

    private fun meaningHit(
        id: String,
        cosine: Float,
        label: String = id,
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId(id),
        memoryId = MemoryId("m-$id"),
        sourceId = SourceId("src"),
        sourceAssetKey = SourceAssetKey(label),
        assetType = AssetType.PDF,
        label = label,
        summaryText = "summary $id",
        score = cosine,
        model = model,
        cosine = cosine,
    )

    private fun lexicalHit(
        id: String,
        label: String,
        excerpt: String,
    ) = MemoryEvidenceSearchHit(
        memoryId = MemoryId("m-$id"),
        revisionId = MemoryRevisionId(id),
        evidenceId = MemoryEvidenceId("e-$id"),
        kind = MemoryEvidenceKind.DOCUMENT_TEXT,
        locator = EvidenceLocator("pdf:page:1"),
        excerpt = excerpt,
        sourceId = SourceId("src"),
        sourceAssetKey = SourceAssetKey(label),
        assetType = AssetType.PDF,
        label = label,
        openPageNumber = 1,
    )
}
