package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningEvidenceLexicalFilterTest {
    @Test
    fun ask_shape_only_cue_names_no_content_token() {
        assertEquals(emptyList<String>(), MeaningRecallCue.contentTokens("which file has"))
        assertEquals(listOf("mira"), MeaningRecallCue.contentTokens("mira"))
    }

    @Test
    fun ignores_stop_words_when_building_required_tokens() {
        assertEquals(
            listOf("scan", "silky"),
            MeaningRecallCue.contentTokens("files with scan and silky"),
        )
        assertEquals(
            listOf("silky"),
            MeaningRecallCue.contentTokens("which file has silky in it"),
        )
    }

    @Test
    fun evidence_must_contain_all_required_tokens() {
        assertTrue(
            satisfies(
                query = "files with scan and silky",
                evidenceText = "Page 2 scan and silky vocabulary",
            ),
        )
        assertFalse(
            satisfies(
                query = "files with scan and silky",
                evidenceText = "Page 1 scan words only",
            ),
        )
    }

    @Test
    fun single_token_query_requires_token_in_evidence() {
        assertTrue(
            satisfies(
                query = "silky",
                evidenceText = "Irregular consonants school anchor silky wreck",
            ),
        )
        assertFalse(
            satisfies(
                query = "silky",
                evidenceText = "Bus Discipline Rules for Students",
            ),
        )
    }

    @Test
    fun show_me_swimming_timetables_matches_singular_evidence() {
        assertEquals(
            listOf("swimming", "timetables"),
            MeaningRecallCue.contentTokens("Show me the files with swimming timetables"),
        )
        assertTrue(
            satisfies(
                query = "Show me the files with swimming timetables",
                evidenceText = "Year 4 swimming timetable Monday to Friday",
            ),
        )
        assertFalse(
            satisfies(
                query = "Show me the files with swimming timetables",
                evidenceText = "Bus Discipline Rules for Students",
            ),
        )
    }

    @Test
    fun empty_token_list_is_vacuously_satisfied() {
        assertTrue(MeaningEvidenceLexicalFilter.satisfies(emptyList(), "anything at all"))
        assertTrue(MeaningEvidenceLexicalFilter.prepare(emptyList()).isEmpty)
    }

    /**
     * A cue compiled once for corpus-wide admission (D-11) must decide exactly
     * what the single-shot call decides.
     */
    @Test
    fun a_prepared_cue_agrees_with_the_single_shot_check() {
        val tokens = MeaningRecallCue.contentTokens("Show me the files with swimming timetables")
        val prepared = MeaningEvidenceLexicalFilter.prepare(tokens)
        listOf(
            "Year 4 swimming timetable Monday to Friday",
            "Swimming only, no timetable here",
            "Bus Discipline Rules for Students",
            "TIMETABLES and SWIMMING in caps",
        ).forEach { evidence ->
            assertEquals(
                "prepared cue disagreed for: $evidence",
                MeaningEvidenceLexicalFilter.satisfies(tokens, evidence),
                prepared.satisfies(evidence),
            )
        }
        assertFalse(prepared.isEmpty)
    }

    /** The exact composition Canonical Recall applies (defect D-10). */
    private fun satisfies(query: String, evidenceText: String): Boolean =
        MeaningEvidenceLexicalFilter.satisfies(
            MeaningRecallCue.contentTokens(query),
            evidenceText,
        )
}
