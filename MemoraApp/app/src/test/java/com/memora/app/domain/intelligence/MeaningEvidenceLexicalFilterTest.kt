package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningEvidenceLexicalFilterTest {
    @Test
    fun requires_at_least_two_content_tokens_before_applying() {
        assertFalse(MeaningEvidenceLexicalFilter.shouldApply("mira"))
        assertTrue(MeaningEvidenceLexicalFilter.shouldApply("scan silky"))
    }

    @Test
    fun ignores_stop_words_when_building_required_tokens() {
        assertEquals(
            listOf("scan", "silky"),
            MeaningEvidenceLexicalFilter.requiredContentTokens("files with scan and silky"),
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
    fun single_token_query_does_not_filter() {
        assertTrue(
            MeaningEvidenceLexicalFilter.evidenceSatisfies(
                query = "mira",
                evidenceText = "No shared words here",
            ),
        )
    }
}
