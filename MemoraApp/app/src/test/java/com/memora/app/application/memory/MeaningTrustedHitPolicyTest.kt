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

    /**
     * D-20 device: Exact files cap at 1.0 after the token boost, so a timetable
     * at 0.55 is outside 0.22. Mixed seating must still keep it.
     */
    @Test
    fun mixed_query_keeps_a_distant_partial_when_exact_hits_fill_the_band() {
        val hits = listOf(
            hit("exact-a", 1.0f, "Swimming schedule term 2"),
            hit("exact-b", 0.98f, "Swimming schedule printout"),
            hit("exact-c", 0.96f, "Swimming schedule screenshot"),
            hit("exact-d", 0.95f, "Swimming schedule heading"),
            hit("exact-e", 0.94f, "Swimming schedule list"),
            hit("timetable", 0.55f, "Grade 2 Swimming timetable 2026 PERIOD TIME"),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 10,
            rawQuery = "swimming schedule",
        )
        assertTrue(trusted.any { it.revisionId.value == "timetable" })
        assertTrue(trusted.any { it.summaryText.contains("schedule") })
        assertEquals(
            MeaningTrustedHitPolicy.MAX_TRUSTED_HITS,
            trusted.size,
        )
    }

    @Test
    fun one_word_query_still_uses_the_score_band() {
        val hits = listOf(
            hit("a", 0.9f, "Passport bio page"),
            hit("b", 0.5f, "Passport renewal notes"),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 10,
            rawQuery = "passport",
        )
        assertEquals(listOf("a"), trusted.map { it.revisionId.value })
    }

    /**
     * Device: a high-cosine JPG with only `schedule` sat above swimming files.
     * Keyword Find already has the timetable PDFs. Reserved Partial seats must
     * prefer the modifier (`swimming`) over the generic head (`schedule`).
     */
    @Test
    fun mixed_query_prefers_modifier_partials_over_a_head_only_image() {
        val hits = listOf(
            hit("schedule-jpg", 1.0f, "Weekly schedule only"),
            hit("swimming-shot", 0.80f, "Swimming time table on the board"),
            hit("exact-a", 0.78f, "Swimming schedule term 2"),
            hit("exact-b", 0.77f, "Swimming schedule printout"),
            hit("exact-c", 0.76f, "Swimming schedule screenshot"),
            hit("timetable-pdf", 0.50f, "Grade 2 Swimming timetable 2026 PERIOD TIME"),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 10,
            rawQuery = "swimming schedule",
        )
        val ids = trusted.map { it.revisionId.value }
        assertTrue(ids.contains("swimming-shot"))
        assertTrue(ids.contains("timetable-pdf"))
        assertTrue(ids.contains("exact-a"))
        assertFalse(ids.contains("schedule-jpg"))
        assertEquals(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS, trusted.size)
    }

    /** If nothing has the modifier, a head-only Partial may still take a seat. */
    @Test
    fun mixed_query_keeps_a_head_only_partial_when_no_modifier_partial_exists() {
        val hits = listOf(
            hit("schedule-jpg", 1.0f, "Weekly schedule only"),
            hit("exact-a", 0.78f, "Swimming schedule term 2"),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 10,
            rawQuery = "swimming schedule",
        )
        assertEquals(listOf("schedule-jpg", "exact-a"), trusted.map { it.revisionId.value })
    }

    /**
     * Device: `swimming timetable` (Exact) still showed only screenshots.
     * Keyword Find already returns the PDFs. Swap image seats for documents
     * still in the ranked pool.
     */
    @Test
    fun exact_query_swaps_image_seats_for_a_pdf_still_in_the_pool() {
        val hits = listOf(
            hit("shot-a", 1.0f, "Swimming timetable board", AssetType.SCREENSHOT),
            hit("shot-b", 0.99f, "Swimming timetable photo", AssetType.SCREENSHOT),
            hit("shot-c", 0.98f, "Swimming timetable crop", AssetType.SCREENSHOT),
            hit("shot-d", 0.97f, "Swimming timetable print", AssetType.SCREENSHOT),
            hit("shot-e", 0.96f, "Swimming timetable screen", AssetType.SCREENSHOT),
            hit(
                "timetable-pdf",
                0.50f,
                "Grade 2 Swimming timetable 2026 PERIOD TIME",
                AssetType.PDF,
            ),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 10,
            rawQuery = "swimming timetable",
        )
        val ids = trusted.map { it.revisionId.value }
        assertTrue(ids.contains("timetable-pdf"))
        assertTrue(trusted.any { it.assetType == AssetType.PDF })
        assertEquals(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS, trusted.size)
    }

    /**
     * Four timetable PDFs in the ranked pool should all take seats in the
     * shown five. Meaning is still a short list — not keyword's full hit set
     * of nine — but it must not keep only one original.
     */
    @Test
    fun exact_query_gives_every_pooled_pdf_a_seat_up_to_the_five() {
        val hits = listOf(
            hit("shot-a", 1.0f, "Swimming timetable board", AssetType.SCREENSHOT),
            hit("shot-b", 0.99f, "Swimming timetable photo", AssetType.SCREENSHOT),
            hit("shot-c", 0.98f, "Swimming timetable crop", AssetType.SCREENSHOT),
            hit("shot-d", 0.97f, "Swimming timetable print", AssetType.SCREENSHOT),
            hit("shot-e", 0.96f, "Swimming timetable screen", AssetType.SCREENSHOT),
            hit("pdf-1", 0.50f, "Grade 2 Swimming timetable 2026", AssetType.PDF),
            hit("pdf-2", 0.48f, "Grade 3 Swimming timetable 2026", AssetType.PDF),
            hit("pdf-3", 0.46f, "Grade 4 Swimming timetable 2026", AssetType.PDF),
            hit("pdf-4", 0.44f, "Grade 5 Swimming timetable 2026", AssetType.PDF),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 10,
            rawQuery = "swimming timetable",
        )
        val pdfIds = trusted.filter { it.assetType == AssetType.PDF }.map { it.revisionId.value }
        assertEquals(listOf("pdf-1", "pdf-2", "pdf-3", "pdf-4"), pdfIds)
        assertEquals(1, trusted.count { it.assetType == AssetType.SCREENSHOT })
        assertEquals(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS, trusted.size)
    }

    /**
     * Device: `swimming schedule` showed swimming screenshots, never the PDF.
     * The PDF is a modifier Partial and must take a swapped document seat.
     */
    @Test
    fun mixed_query_swaps_an_image_seat_for_the_timetable_pdf() {
        val hits = listOf(
            hit("swimming-shot", 0.90f, "Swimming time table on the board", AssetType.SCREENSHOT),
            hit("swimming-img", 0.88f, "Swimming timetable photo", AssetType.PHOTO),
            hit("exact-a", 0.80f, "Swimming schedule term 2", AssetType.SCREENSHOT),
            hit("exact-b", 0.79f, "Swimming schedule printout", AssetType.SCREENSHOT),
            hit("exact-c", 0.78f, "Swimming schedule heading", AssetType.SCREENSHOT),
            hit(
                "timetable-pdf",
                0.45f,
                "Grade 2 Swimming timetable 2026 PERIOD TIME",
                AssetType.PDF,
            ),
        )
        val trusted = MeaningTrustedHitPolicy.apply(
            hits = hits,
            limit = 10,
            rawQuery = "swimming schedule",
        )
        val ids = trusted.map { it.revisionId.value }
        assertTrue(ids.contains("timetable-pdf"))
        assertTrue(ids.contains("swimming-shot"))
        assertFalse(ids.contains("exact-c"))
        assertEquals(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS, trusted.size)
    }

    private fun hit(
        id: String,
        score: Float,
        summary: String = "text $id",
        assetType: AssetType = AssetType.PDF,
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
    )
}
