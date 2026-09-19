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
    fun exact_originals_lead_and_self_captures_stay_last() {
        val exact = (0 until 4).map { index ->
            hit("tt-$index", "weekly swimming timetable $index")
        }
        val self = (0 until 3).map { index ->
            hit(
                "self-$index",
                "swimming timetable Find by meaning Why this result? PDF memory $index",
            )
        }
        val neighbour = hit("photo", "pool swimming photo")
        val ordered = MeaningNamedWordDepthPage.order(
            hits = self + exact + neighbour,
            rawQuery = "swimming timetable",
        )
        assertEquals(
            listOf("tt-0", "tt-1", "tt-2", "tt-3", "photo", "self-0", "self-1", "self-2"),
            ordered.map { it.revisionId.value },
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
    fun deepest_partial_leads_shallower_families_on_a_three_word_cue() {
        val gradeOnly = (0 until 20).map { index ->
            hit("grade-$index", "term grade report $index")
        }
        val gold = hit("tt", "grade 2 weekly swimming timetable")
        val ordered = MeaningNamedWordDepthPage.order(
            hits = gradeOnly + gold,
            rawQuery = "when are the swimming classes for grade 2",
        )
        assertEquals("tt", ordered.first().revisionId.value)
        assertEquals("grade-0", ordered[1].revisionId.value)
        assertTrue(ordered.takeLast(20).all { it.revisionId.value.startsWith("grade-") })
    }

    @Test
    fun qualifier_hits_lead_class_only_photos_on_the_same_head() {
        val classesSwim = (0 until 10).map { index ->
            hit("class-$index", "swimming classes photo $index")
        }
        val gold = hit("tt", "grade 2 weekly swimming timetable")
        val ordered = MeaningNamedWordDepthPage.order(
            hits = classesSwim + gold,
            rawQuery = "when are the swimming classes for grade 2",
        )
        assertEquals("tt", ordered.first().revisionId.value)
        assertEquals("class-0", ordered[1].revisionId.value)
    }

    @Test
    fun starved_mix_runs_only_among_the_deepest_families() {
        val classesSwim = (0 until 10).map { index ->
            hit("class-$index", "swimming classes photo $index")
        }
        val gradeSwim = listOf(
            hit("tt-a", "grade 2 weekly swimming timetable A"),
            hit("tt-b", "grade 2 weekly swimming timetable B"),
        )
        val gradeOnly = (0 until 10).map { index ->
            hit("grade-$index", "term grade report $index")
        }
        val ordered = MeaningNamedWordDepthPage.order(
            hits = classesSwim + gradeSwim + gradeOnly,
            rawQuery = "when are the swimming classes for grade 2",
        )
        assertEquals(
            listOf("tt-a", "tt-b") + (0 until 10).map { "class-$it" },
            ordered.take(12).map { it.revisionId.value },
        )
        assertTrue(ordered.takeLast(10).all { it.revisionId.value.startsWith("grade-") })
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
    fun self_captures_do_not_form_the_exact_family() {
        val self = (0 until 12).map { index ->
            hit(
                "self-$index",
                "scan silky Find by meaning Why this result? PDF memory $index",
            )
        }
        val scan = hit("scan", "document scan pages")
        val silky = hit("silky", "silky fabric invoice")
        val ordered = MeaningNamedWordDepthPage.order(
            hits = self + scan + silky,
            rawQuery = "scan silky",
        )
        assertEquals(
            listOf("scan", "silky") + self.map { it.revisionId.value },
            ordered.map { it.revisionId.value },
        )
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
