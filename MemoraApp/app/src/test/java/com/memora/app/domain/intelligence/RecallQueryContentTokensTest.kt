package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
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
}
