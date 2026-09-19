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

class MeaningNamedWordDepthPageTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun one_word_cue_keeps_incoming_order() {
        val hits = listOf(
            hit("a", "passport scan"),
            hit("b", "passport photo"),
            hit("c", "passport copy"),
        )
        assertEquals(hits, MeaningNamedWordDepthPage.order(hits, "passport"))
    }

    @Test
    fun single_family_keeps_incoming_order() {
        val hits = (0 until 5).map { index ->
            hit("both-$index", "swimming classes photo $index")
        }
        assertEquals(
            hits.map { it.revisionId.value },
            MeaningNamedWordDepthPage.order(hits, "when are the swimming classes")
                .map { it.revisionId.value },
        )
    }

    @Test
    fun exact_family_leads_then_named_word_neighbours() {
        val twoToken = (0 until 3).map { index ->
            hit("class-$index", "swimming classes photo $index")
        }
        val oneToken = hit("tt", "weekly swimming timetable")
        val ordered = MeaningNamedWordDepthPage.order(
            hits = twoToken + oneToken,
            rawQuery = "when are the swimming classes",
        )
        assertEquals(
            listOf("class-0", "class-1", "class-2", "tt"),
            ordered.map { it.revisionId.value },
        )
    }

    @Test
    fun does_not_prefer_a_pdf_over_another_type_at_the_same_depth() {
        val shot = hit("shot", "swimming timetable photo", AssetType.SCREENSHOT)
        val pdf = hit("pdf", "swimming timetable pages", AssetType.PDF)
        val note = hit("note", "swimming timetable note", AssetType.NOTE)
        val ordered = MeaningNamedWordDepthPage.order(
            hits = listOf(shot, pdf, note),
            rawQuery = "swimming timetable",
        )
        assertEquals(listOf("shot", "pdf", "note"), ordered.map { it.revisionId.value })
    }

    @Test
    fun same_depth_families_share_the_page() {
        val classesOnly = (0 until 20).map { index ->
            hit("classes-$index", "beginner classes list $index")
        }
        val swimmingOnly = hit("tt", "weekly swimming timetable")
        val ordered = MeaningNamedWordDepthPage.order(
            hits = classesOnly + swimmingOnly,
            rawQuery = "when are the swimming classes",
        )
        assertEquals("tt", ordered.first().revisionId.value)
        assertEquals("classes-0", ordered[1].revisionId.value)
        assertTrue(ordered.take(20).any { it.revisionId.value == "tt" })
    }

    @Test
    fun starved_family_takes_its_fair_share_before_the_cosine_majority() {
        val classesOnly = (0 until 20).map { index ->
            hit("classes-$index", "beginner classes list $index")
        }
        val swimmingPhotos = (0 until 3).map { index ->
            hit("swim-$index", "pool swimming photo $index")
        }
        val timetables = listOf(
            hit("tt-a", "weekly swimming timetable A"),
            hit("tt-b", "weekly swimming timetable B"),
        )
        val ordered = MeaningNamedWordDepthPage.order(
            hits = classesOnly + swimmingPhotos + timetables,
            rawQuery = "when are the swimming classes",
        )
        assertEquals(
            listOf("swim-0", "swim-1", "swim-2", "tt-a", "tt-b"),
            ordered.take(5).map { it.revisionId.value },
        )
        assertEquals("classes-0", ordered[5].revisionId.value)
    }

    @Test
    fun does_not_treat_classes_as_timetable() {
        val classesOnly = hit("class", "swimming classes.jpg")
        val ordered = MeaningNamedWordDepthPage.order(
            hits = listOf(classesOnly),
            rawQuery = "when are the swimming classes",
        )
        assertEquals(1, ordered.size)
        assertTrue(ordered.single().summaryText.contains("classes"))
    }

    private fun hit(
        id: String,
        summary: String,
        assetType: AssetType = AssetType.PDF,
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId(id),
        memoryId = MemoryId("m-$id"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey(id),
        assetType = assetType,
        label = id,
        summaryText = summary,
        score = 0.8f,
        model = model,
        cosine = 0.8f,
    )
}
