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

    /**
     * D-10: a time word constrains which Memories qualify; it is not text to
     * retrieve on. Embedding it pulled the query vector toward recency language
     * and pushed the one file that says `silky` out of the candidate pool, so
     * `recent files with silky` answered nothing while bare `silky` worked.
     */
    @Test
    fun embed_text_drops_the_words_a_time_constraint_owns() {
        assertEquals("silky", MeaningRecallCue.embedText("recent files with silky"))
        assertEquals(listOf("silky"), MeaningRecallCue.contentTokens("recent files with silky"))

        assertEquals("notes", MeaningRecallCue.embedText("notes in 2024"))
        assertEquals(listOf("notes"), MeaningRecallCue.contentTokens("notes in 2024"))

        assertEquals("invoice", MeaningRecallCue.embedText("invoice from last week"))
    }

    /**
     * The invariant behind D-10: candidate generation and the lexical precision
     * gate read one derivation, so they can never require a word the query
     * vector was not built from.
     */
    @Test
    fun embed_text_is_exactly_the_content_tokens_it_will_be_filtered_on() {
        listOf(
            "recent files with silky",
            "notes in 2024",
            "Show me the files with swimming timetables",
            "which file has silky in it?",
            "get me some e.g.s from the pdf related to the training project",
            "scan silky",
        ).forEach { query ->
            val tokens = MeaningRecallCue.contentTokens(query)
            assertEquals(
                "embed text drifted from required tokens for: $query",
                tokens.joinToString(" "),
                MeaningRecallCue.embedText(query),
            )
        }
    }

    /**
     * A cue made only of time words names no content. Meaning Find answers an
     * honest empty rather than listing cosine neighbours of the word `recent`;
     * retrieving by TIME alone is a later job (I5).
     */
    @Test
    fun a_time_only_cue_names_no_content() {
        assertEquals(emptyList<String>(), MeaningRecallCue.contentTokens("recent files"))
        assertEquals(
            emptyList<String>(),
            MeaningRecallCue.contentTokens("screenshots from last week"),
        )
    }
}
