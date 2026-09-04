package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningEvidenceLexicalFilterTest {
    @Test
    fun applies_when_cue_has_at_least_one_content_token() {
        assertTrue(MeaningEvidenceLexicalFilter.shouldApply("mira"))
        assertTrue(MeaningEvidenceLexicalFilter.shouldApply("scan silky"))
        assertFalse(MeaningEvidenceLexicalFilter.shouldApply("which file has"))
    }

    @Test
    fun ignores_stop_words_when_building_required_tokens() {
        assertEquals(
            listOf("scan", "silky"),
            MeaningEvidenceLexicalFilter.requiredContentTokens("files with scan and silky"),
        )
        assertEquals(
            listOf("silky"),
            MeaningEvidenceLexicalFilter.requiredContentTokens("which file has silky in it"),
        )
    }

    @Test
    fun evidence_must_contain_all_required_tokens() {
        assertTrue(
            MeaningEvidenceLexicalFilter.evidenceSatisfies(
                query = "files with scan and silky",
                evidenceText = "Page 2 scan and silky vocabulary",
            ),
        )
        assertFalse(
            MeaningEvidenceLexicalFilter.evidenceSatisfies(
                query = "files with scan and silky",
                evidenceText = "Page 1 scan words only",
            ),
        )
    }

    @Test
    fun single_token_query_requires_token_in_evidence() {
        assertTrue(
            MeaningEvidenceLexicalFilter.evidenceSatisfies(
                query = "silky",
                evidenceText = "Irregular consonants school anchor silky wreck",
            ),
        )
        assertFalse(
            MeaningEvidenceLexicalFilter.evidenceSatisfies(
                query = "silky",
                evidenceText = "Bus Discipline Rules for Students",
            ),
        )
    }
}
