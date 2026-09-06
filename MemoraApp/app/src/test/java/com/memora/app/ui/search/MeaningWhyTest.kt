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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningWhyTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun exact_cue_names_only_the_words_this_file_has() {
        val why = MeaningWhy.explain(
            hit = hit(summary = "Irregular consonants school anchor silky wreck cook"),
            query = "silky",
        )
        assertEquals(listOf("silky"), why.matched)
        assertEquals(emptyList<String>(), why.missing)
        assertEquals("Has \"silky\".", MeaningWhy.coverageText(why.matched, why.missing))
        assertFalse(MeaningWhy.plainText(why).contains("cue words", ignoreCase = true))
        assertFalse(MeaningWhy.plainText(why).contains("You asked about"))
    }

    @Test
    fun partial_cue_names_the_missing_word_and_never_claims_it_appeared() {
        val why = MeaningWhy.explain(
            hit = hit(summary = "Grade 2 Swimming Timetable 2026 PERIOD TIME"),
            query = "swimming schedule",
        )
        assertEquals(listOf("swimming"), why.matched)
        assertEquals(listOf("schedule"), why.missing)
        val text = MeaningWhy.plainText(why)
        assertTrue(text.contains("Has \"swimming\"."))
        assertTrue(text.contains("Does not have \"schedule\"."))
        assertFalse(text.contains("Your cue words appear"))
        assertFalse(text.contains("Found by meaning"))
    }

    /**
     * U5: do not repeat the card snippet when the matching word is already
     * visible there.
     */
    @Test
    fun omits_a_cited_line_when_the_card_already_shows_the_word() {
        val why = MeaningWhy.explain(
            hit = hit(summary = "Grade 2 Swimming Timetable 2026"),
            query = "swimming schedule",
        )
        assertNull(why.citedLine)
    }

    /**
     * D-7 start: when the card snippet does not carry the word but another
     * stored span does, Why quotes that span instead of the cosine winner.
     */
    @Test
    fun cites_the_hidden_span_when_the_card_snippet_does_not_carry_the_word() {
        val why = MeaningWhy.explain(
            hit = hit(
                summary = "G Recommended Launch Plan To balance engineering complexity",
                precisionText = "G Recommended Launch Plan. Shop 41 Invoice No. SDA/PLZ/25/04031 due date.",
            ),
            query = "pterodactyl invoice",
        )
        assertEquals(listOf("invoice"), why.matched)
        assertEquals(listOf("pterodactyl"), why.missing)
        assertTrue(why.citedLine!!.contains("Invoice", ignoreCase = true))
        assertFalse(why.citedLine!!.contains("Launch Plan"))
    }

    @Test
    fun two_matched_words_read_as_a_list() {
        assertEquals(
            "Has \"swimming\" and \"timetable\".",
            MeaningWhy.coverageText(listOf("swimming", "timetable"), emptyList()),
        )
    }

    private fun hit(
        summary: String,
        precisionText: String = "",
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId("rev"),
        memoryId = MemoryId("mem"),
        sourceId = SourceId("src"),
        sourceAssetKey = SourceAssetKey("asset"),
        assetType = AssetType.PDF,
        label = "Grade-2-Swimming-TT-2026.pdf",
        summaryText = summary,
        score = 0.5f,
        model = model,
        precisionText = precisionText,
    )
}
