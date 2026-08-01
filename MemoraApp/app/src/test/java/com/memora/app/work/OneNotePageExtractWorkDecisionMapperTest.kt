package com.memora.app.work

import com.memora.app.application.notes.PendingOneNotePageExtractOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneNotePageExtractWorkDecisionMapperTest {

    @Test
    fun mapsPersistedWithMoreToContinue() {
        val decision = OneNotePageExtractWorkDecisionMapper.map(
            PendingOneNotePageExtractOutcome.Persisted(
                processedSourceAssetKey = "p1",
                hasMorePending = true,
            ),
        )
        assertTrue(decision is OneNotePageExtractWorkDecision.Continue)
        assertEquals("p1", (decision as OneNotePageExtractWorkDecision.Continue).afterSourceAssetKey)
    }

    @Test
    fun mapsNoPendingToCompleted() {
        assertEquals(
            OneNotePageExtractWorkDecision.CompletedDrain,
            OneNotePageExtractWorkDecisionMapper.map(PendingOneNotePageExtractOutcome.NoPending),
        )
    }
}
