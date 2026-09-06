package com.memora.app.ui.search

import com.memora.app.application.memory.CanonicalRecallRetrievalPath
import com.memora.app.application.memory.CanonicalRecallTestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalRecallWhyCopyTest {
    @Test
    fun keyword_why_labels_path_and_cites_excerpt() {
        val why = CanonicalRecallWhyCopy.whyThisResult(
            result = CanonicalRecallTestFixtures.keywordRecall(
                label = "memora-persist-fixture.pdf",
                excerpt = "Café memory: meet Mira at 10:30…",
                pageNumber = 1,
            ),
            query = "meet mira",
        )

        assertTrue(why.startsWith(CanonicalRecallWhyCopy.WHY_THIS_RESULT_LABEL))
        assertTrue(why.contains("meet mira"))
        assertTrue(why.contains("memora-persist-fixture.pdf"))
        assertTrue(why.contains("Café memory: meet Mira at 10:30…"))
        assertTrue(why.contains("Page 1"))
        assertTrue(why.contains("exact words"))
        assertFalse(why.contains("confidence"))
    }

    @Test
    fun meaning_why_names_the_words_in_the_excerpt_and_nothing_else() {
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
        assertFalse(why.contains("Your cue words appear"))
        assertFalse(why.contains("Has \"silky\""))
        assertFalse(why.contains("Found by meaning"))
        assertFalse(why.contains("cue-best"))
        assertFalse(why.contains("Score reflects"))
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
    fun friendly_display_label_strips_storage_hash_prefix() {
        assertEquals(
            "Spelling list 4 (1)-1.pdf",
            CanonicalRecallWhyCopy.friendlyDisplayLabel(
                "500fb02768e7a4bc0b91977e9d346a14774802600_Spelling list 4 (1)-1.pdf",
            ),
        )
    }
}
