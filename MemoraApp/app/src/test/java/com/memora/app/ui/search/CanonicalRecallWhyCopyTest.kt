package com.memora.app.ui.search

import com.memora.app.application.memory.CanonicalRecallRetrievalPath
import com.memora.app.application.memory.CanonicalRecallTestFixtures
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
        assertTrue(why.contains("PDF page 1"))
        assertTrue(why.contains("exact words"))
        assertTrue(why.contains("saved evidence"))
        assertFalse(why.contains("confidence"))
    }

    @Test
    fun meaning_why_labels_path_and_score() {
        val why = CanonicalRecallWhyCopy.whyThisResult(
            result = CanonicalRecallTestFixtures.keywordRecall(
                label = "doc.pdf",
                excerpt = "Hotel confirmation near the cafe district",
                pageNumber = 3,
            ).copy(
                retrievalPath = CanonicalRecallRetrievalPath.MEANING,
                rankScore = 0.5f,
                evidenceTokenBoosted = true,
            ),
            query = "mira",
        )

        assertTrue(why.contains("on-device meaning similarity"))
        assertTrue(why.contains("appears in this saved evidence"))
        assertTrue(why.contains("meaning similarity"))
    }
}
