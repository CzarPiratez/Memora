package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Test

class MeaningRecallCueTest {
    @Test
    fun embed_text_uses_content_tokens_for_natural_language_questions() {
        assertEquals("silky", MeaningRecallCue.embedText("which file has silky in it?"))
        assertEquals("silky wreck", MeaningRecallCue.embedText("which file has silky and wreck in it"))
    }

    @Test
    fun embed_text_keeps_normalized_raw_when_no_content_tokens() {
        assertEquals("which file has", MeaningRecallCue.embedText("  which   file  has  "))
    }

    @Test
    fun normalize_collapses_whitespace_and_caps_length() {
        val long = "a".repeat(200)
        assertEquals(120, MeaningRecallCue.normalize(long).length)
        assertEquals("silky wreck", MeaningRecallCue.displayQuery("  silky   wreck  "))
    }

    @Test
    fun content_tokens_drop_question_wrappers() {
        assertEquals(
            listOf("silky"),
            MeaningRecallCue.contentTokens("which file has silky in it"),
        )
    }

    @Test
    fun embed_text_uses_content_tokens_for_show_me_paraphrase() {
        assertEquals(
            "swimming timetables",
            MeaningRecallCue.embedText("Show me the files with swimming timetables"),
        )
        assertEquals(
            listOf("swimming", "timetables"),
            MeaningRecallCue.contentTokens("Show me the files with swimming timetables"),
        )
        assertEquals(
            "training project",
            MeaningRecallCue.embedText(
                "get me some e.g.s from the pdf related to the training project",
            ),
        )
    }
}
