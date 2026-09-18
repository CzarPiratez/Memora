package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningSearchGoldLocatorTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun cue_for_matches_registered_swimming_classes_ignoring_case() {
        val cue = MeaningSearchGoldLocator.cueFor("When are the swimming classes")
        assertEquals("when are the swimming classes", cue?.query)
        assertTrue(cue?.goldSubstrings.orEmpty().any { it.contains("timetable") })
    }

    @Test
    fun cue_for_unregistered_query_is_null() {
        assertNull(MeaningSearchGoldLocator.cueFor("scan silky"))
    }

    @Test
    fun locate_unregistered_query_is_not_a_gold_cue() {
        val membership = MeaningSearchGoldLocator.locate(
            rawQuery = "scan silky",
            collapseHits = listOf(hit("tt", "Grade-2-Swimming-TT-2026.pdf")),
            admittedHits = emptyList(),
        )
        assertFalse(membership.registered)
        assertEquals(
            MeaningSearchGoldLocator.DIAGNOSIS_NO_GOLD_CUE,
            MeaningSearchGoldLocator.diagnosis(membership, rankedRank = null, shownRank = null),
        )
    }

    @Test
    fun locate_registered_query_with_no_filename_match_is_not_in_collapse() {
        val membership = MeaningSearchGoldLocator.locate(
            rawQuery = "when are the swimming classes",
            collapseHits = listOf(hit("class", "swimming classes.jpg")),
            admittedHits = listOf(hit("class", "swimming classes.jpg")),
        )
        assertTrue(membership.registered)
        assertNull(membership.collapseRank)
        assertEquals(
            MeaningSearchGoldLocator.DIAGNOSIS_NOT_IN_COLLAPSE,
            MeaningSearchGoldLocator.diagnosis(membership, rankedRank = null, shownRank = null),
        )
    }

    @Test
    fun locate_best_gold_in_collapse_but_outside_admitted_is_out_of_pool() {
        val photo = hit("class", "swimming classes.jpg", cosine = 0.9f)
        val pdf = hit("tt", "Grade-2-Swimming-TT-2026.pdf", cosine = 0.4f)
        val membership = MeaningSearchGoldLocator.locate(
            rawQuery = "when are the swimming classes",
            collapseHits = listOf(photo, pdf),
            admittedHits = listOf(photo),
        )
        assertEquals("Grade-2-Swimming-TT-2026.pdf", membership.goldLabel)
        assertEquals(AssetType.PDF.name, membership.goldAssetType)
        assertEquals(2, membership.collapseRank)
        assertNull(membership.admittedRank)
        assertEquals(
            MeaningSearchGoldLocator.DIAGNOSIS_OUT_OF_POOL,
            MeaningSearchGoldLocator.diagnosis(membership, rankedRank = null, shownRank = null),
        )
    }

    @Test
    fun locate_does_not_treat_classes_as_a_timetable_synonym() {
        val membership = MeaningSearchGoldLocator.locate(
            rawQuery = "when are the swimming classes",
            collapseHits = listOf(hit("note", "swimming classes note.txt")),
            admittedHits = listOf(hit("note", "swimming classes note.txt")),
        )
        assertTrue(membership.registered)
        assertNull(membership.collapseRank)
    }

    @Test
    fun diagnosis_distinguishes_tier_drop_page_miss_and_on_page() {
        val pdf = hit("tt", "Grade-2-Swimming-TT-2026.pdf")
        val membership = MeaningSearchGoldLocator.locate(
            rawQuery = "when are the swimming classes",
            collapseHits = listOf(pdf),
            admittedHits = listOf(pdf),
        )
        assertEquals(1, membership.admittedRank)
        assertEquals(
            MeaningSearchGoldLocator.DIAGNOSIS_DROPPED_BY_TIER,
            MeaningSearchGoldLocator.diagnosis(membership, rankedRank = null, shownRank = null),
        )
        assertEquals(
            MeaningSearchGoldLocator.DIAGNOSIS_IN_POOL_OFF_PAGE,
            MeaningSearchGoldLocator.diagnosis(membership, rankedRank = 12, shownRank = null),
        )
        assertEquals(
            MeaningSearchGoldLocator.DIAGNOSIS_ON_PAGE,
            MeaningSearchGoldLocator.diagnosis(membership, rankedRank = 4, shownRank = 4),
        )
    }

    @Test
    fun matches_gold_on_source_asset_key_not_only_label() {
        val hit = hit(
            id = "key",
            label = "scan001.pdf",
            sourceKey = "Grade-2-Swimming-TT-2026.pdf",
        )
        assertTrue(
            MeaningSearchGoldLocator.matchesGold(
                hit,
                listOf("swimming-tt"),
            ),
        )
    }

    private fun hit(
        id: String,
        label: String,
        cosine: Float = 0.5f,
        sourceKey: String = id,
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId(id),
        memoryId = MemoryId("m-$id"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey(sourceKey),
        assetType = AssetType.PDF,
        label = label,
        summaryText = "stored summary",
        score = cosine,
        model = model,
        cosine = cosine,
    )
}
