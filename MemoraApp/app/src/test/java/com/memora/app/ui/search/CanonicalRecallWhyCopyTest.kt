package com.memora.app.ui.search

import com.memora.app.application.memory.CanonicalRecallRetrievalPath
import com.memora.app.application.memory.CanonicalRecallTestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalRecallWhyCopyTest {
    @Test
    fun keyword_why_names_the_words_and_the_path_without_repeating_the_card() {
        val result = CanonicalRecallTestFixtures.keywordRecall(
            label = "memora-persist-fixture.pdf",
            excerpt = "Café memory: meet Mira at 10:30…",
            pageNumber = 1,
        )
        val why = CanonicalRecallWhyCopy.present(result, "meet mira")

        assertEquals("You asked about ", why.askPrefix)
        assertEquals("\"meet mira\"", why.ask)
        assertEquals("Those words are ", why.subjectPrefix)
        assertEquals("in this file's saved text", why.subject)
        assertEquals(null, why.citedLine)
        assertEquals(CanonicalRecallWhyCopy.FOUND_BY_KEYWORD, why.howFound)
        assertTrue(why.spokenText.contains("meet mira"))
        assertTrue(why.spokenText.contains("exact words"))
        assertFalse(why.spokenText.contains(result.label))
        assertFalse(why.spokenText.contains(result.excerpt))
        assertFalse(why.spokenText.contains("Page 1"))
        assertFalse(why.spokenText.contains("Why this result?"))
        assertFalse(why.spokenText.contains("Matched "))
        assertFalse(why.spokenText.contains("confidence"))
        assertFalse(why.spokenText.contains("meaning"))
    }

    @Test
    fun keyword_why_uses_singular_word_when_the_cue_is_one_token() {
        val why = CanonicalRecallWhyCopy.present(
            result = CanonicalRecallTestFixtures.keywordRecall(excerpt = "silky wreck cook"),
            query = "silky",
        )
        assertEquals("That word is ", why.subjectPrefix)
        assertEquals("\"silky\"", why.ask)
    }

    @Test
    fun meaning_why_pairs_the_ask_with_the_file_and_names_the_path() {
        val why = CanonicalRecallWhyCopy.whyThisResult(
            result = CanonicalRecallTestFixtures.keywordRecall(
                label = "500fb02768e7a4bc0b91977e9d346a14774802600_Spelling list 4 (1)-1.pdf",
                excerpt = "Irregular consonants school anchor silky wreck cook",
                pageNumber = 1,
            ).copy(
                retrievalPath = CanonicalRecallRetrievalPath.MEANING,
                rankScore = 0.5f,
                evidenceTokenBoosted = true,
            ),
            query = "silky wreck",
        )

        assertTrue(why.contains("You asked about a silky wreck."))
        assertTrue(why.contains("This file is"))
        assertTrue(why.contains(CanonicalRecallWhyCopy.FOUND_BY_MEANING_WITH_WORD_ASSIST))
        assertFalse(why.contains("Your cue words appear"))
        assertFalse(why.contains("Has \"silky\""))
        assertFalse(why.contains("Page 1"))
        assertFalse(why.contains("cue-best"))
        assertFalse(why.contains("Score reflects"))
        assertFalse(why.contains("Why this result?"))
    }

    @Test
    fun meaning_why_does_not_inventory_missing_words() {
        val why = CanonicalRecallWhyCopy.whyThisResult(
            result = CanonicalRecallTestFixtures.keywordRecall(
                label = "Grade-2-Swimming-TT-2026.pdf",
                excerpt = "Grade 2 Swimming Timetable 2026",
                pageNumber = 1,
            ).copy(
                retrievalPath = CanonicalRecallRetrievalPath.MEANING,
                rankScore = 0.5f,
                evidenceTokenBoosted = true,
            ),
            query = "swimming schedule",
        )

        assertTrue(why.contains("You asked about a swimming schedule."))
        assertTrue(why.contains("This file is Grade 2 Swimming TT 2026."))
        assertFalse(why.contains("Does not have"))
        assertFalse(why.contains("Your cue words appear"))
    }

    @Test
    fun unboosted_meaning_why_does_not_claim_a_typed_word_helped() {
        val why = CanonicalRecallWhyCopy.present(
            result = CanonicalRecallTestFixtures.keywordRecall(
                label = "Grade-2-Swimming-TT-2026.pdf",
                excerpt = "Grade 2 Swimming Timetable 2026",
            ).copy(
                retrievalPath = CanonicalRecallRetrievalPath.MEANING,
                rankScore = 0.5f,
                evidenceTokenBoosted = false,
            ),
            query = "swimming timetable",
        )
        assertEquals(CanonicalRecallWhyCopy.FOUND_BY_MEANING, why.howFound)
        assertFalse(why.howFound.contains("helped by a word"))
    }

    @Test
    fun friendly_display_label_strips_storage_hash_prefix() {
        assertEquals(
            "Spelling list 4 (1)-1.pdf",
            CanonicalRecallWhyCopy.friendlyDisplayLabel(
                "500fb02768e7a4bc0b91977e9d346a14774802600_Spelling list 4 (1)-1.pdf",
            ),
        )
    }
}
