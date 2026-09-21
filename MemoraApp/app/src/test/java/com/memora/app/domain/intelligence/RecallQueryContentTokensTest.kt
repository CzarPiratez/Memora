package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecallQueryContentTokensTest {
    @Test
    fun splits_on_commas_and_drops_connectors() {
        assertEquals(
            listOf("silky", "wreck"),
            RecallQueryContentTokens.tokens("silky, wreck"),
        )
        assertEquals(
            listOf("scan", "silky"),
            RecallQueryContentTokens.tokens("scan and silky"),
        )
    }

    @Test
    fun drops_question_words_from_natural_language_cues() {
        assertEquals(
            listOf("silky", "wreck"),
            RecallQueryContentTokens.tokens("which file has silky and wreck in it"),
        )
    }

    @Test
    fun drops_show_me_ask_shape_wrappers() {
        assertEquals(
            listOf("swimming", "timetables"),
            RecallQueryContentTokens.tokens("Show me the files with swimming timetables"),
        )
        assertEquals(
            listOf("silky"),
            RecallQueryContentTokens.tokens("doc with silky in it"),
        )
        assertTrue(RecallQueryContentTokens.tokens("show me the files").isEmpty())
        assertTrue(RecallQueryContentTokens.tokens("give me 2 files").isEmpty())
        assertEquals(
            listOf("grade", "2"),
            RecallQueryContentTokens.tokens("grade 2"),
        )
        assertEquals(
            listOf("training", "project"),
            RecallQueryContentTokens.tokens(
                "get me some e.g.s from the pdf related to the training project",
            ),
        )
    }
}
