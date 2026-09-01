package com.memora.app.application.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningSearchCandidateDiagnosticsTest {
    @Test
    fun toLogMessage_includes_drop_breakdown() {
        val stats = MeaningSearchCandidateDropStats(
            missingLookup = 1,
            dimensionMismatch = 2,
            belowMinScore = 0,
            blankLabelOrSummary = 0,
            missingEvidenceRow = 1,
            blankExcerpt = 0,
        )

        val message = stats.toLogMessage(
            summaryIndexed = 3,
            evidenceIndexed = 4,
            query = "invoice",
        )

        assertTrue(message.contains("query=invoice"))
        assertTrue(message.contains("summaries=3"))
        assertTrue(message.contains("evidenceVectors=4"))
        assertTrue(message.contains("dimensionMismatch=2"))
        assertEquals(4, stats.totalDropped)
    }
}
