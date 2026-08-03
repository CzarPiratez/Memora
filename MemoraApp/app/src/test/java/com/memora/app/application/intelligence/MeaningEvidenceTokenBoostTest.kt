package com.memora.app.application.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningEvidenceTokenBoostTest {
    @Test
    fun boosts_when_cue_token_in_evidence() {
        val (score, boosted) = MeaningEvidenceTokenBoost.apply(
            cosine = 0.2f,
            query = "mira",
            evidenceText = "Page 3 ECHO meet mira follow-up",
        )
        assertTrue(boosted)
        assertEquals(0.55f, score, 0.001f)
    }

    @Test
    fun no_boost_without_token() {
        val (score, boosted) = MeaningEvidenceTokenBoost.apply(
            cosine = 0.4f,
            query = "mira",
            evidenceText = "Page 1 FOXTROT cover sheet",
        )
        assertFalse(boosted)
        assertEquals(0.4f, score, 0.001f)
    }

    @Test
    fun ignores_tiny_tokens() {
        assertFalse(
            MeaningEvidenceTokenBoost.evidenceContainsCueToken("a to", "a note to keep"),
        )
    }
}
