package com.memora.app.ui.search

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

class MeaningWhyTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun relevance_pairs_the_ask_with_what_the_file_calls_itself() {
        val why = MeaningWhy.explain(
            hit = hit(summary = "Grade 2 Swimming Timetable 2026 PERIOD TIME MON TUE"),
            query = "swimming schedule",
        )
        val text = MeaningWhy.plainText(why)
        assertTrue(text.contains("You asked about a swimming schedule."))
        assertTrue(text.contains("This file is Grade 2 Swimming TT 2026."))
        assertTrue(why.citedLine!!.contains("Swimming", ignoreCase = true))
        assertFalse(text.contains("Has \"swimming\""))
        assertFalse(text.contains("Does not have"))
        assertFalse(text.contains("cue words", ignoreCase = true))
    }

    @Test
    fun does_not_claim_schedule_means_timetable() {
        val why = MeaningWhy.explain(
            hit = hit(summary = "Grade 2 Swimming Timetable 2026"),
            query = "swimming schedule",
        )
        val text = MeaningWhy.plainText(why)
        assertTrue(text.contains("swimming schedule"))
        assertTrue(text.contains("Swimming TT") || text.contains("Swimming Timetable"))
        assertFalse(text.contains("schedule means"))
        assertFalse(text.contains("same as"))
    }

    @Test
    fun machine_screenshot_name_falls_back_to_the_saved_line() {
        val why = MeaningWhy.explain(
            hit = hit(
                summary = "Monthly Rent Collection Records Shop 41 Invoice No SDA",
                label = "Screenshot_20260412_131237_Chrome.png",
            ),
            query = "pterodactyl invoice",
        )
        assertEquals("a pterodactyl invoice", why.asked)
        assertEquals("Monthly Rent Collection Records Shop 41 Invoice No SDA", why.fileIs)
        assertTrue(why.citedLine!!.contains("Invoice", ignoreCase = true))
    }

    @Test
    fun asked_phrase_uses_content_tokens_not_the_wrappers() {
        assertEquals(
            "a swimming schedule",
            MeaningWhy.askedPhrase("get me some egs from the pdf related to swimming schedule"),
        )
        assertEquals("a silky", MeaningWhy.askedPhrase("which file has silky in it?"))
    }

    @Test
    fun first_readable_clause_stops_before_timetable_grid_noise() {
        assertEquals(
            "Grade 2 Swimming Timetable 2026",
            MeaningWhy.firstReadableClause("Grade 2 Swimming Timetable 2026 PERIOD TIME MON TUE WED"),
        )
    }

    private fun hit(
        summary: String,
        precisionText: String = "",
        label: String = "Grade-2-Swimming-TT-2026.pdf",
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId("rev"),
        memoryId = MemoryId("mem"),
        sourceId = SourceId("src"),
        sourceAssetKey = SourceAssetKey("asset"),
        assetType = AssetType.PDF,
        label = label,
        summaryText = summary,
        score = 0.5f,
        model = model,
        precisionText = precisionText,
    )
}
