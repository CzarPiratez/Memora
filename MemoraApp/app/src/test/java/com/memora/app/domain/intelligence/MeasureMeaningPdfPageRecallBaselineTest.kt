package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasureMeaningPdfPageRecallBaselineTest {
    @Test
    fun m1_report_records_cosine_gap_and_boost_recovery() {
        val report = MeasureMeaningPdfPageRecallBaseline.measure()
        assertEquals(MeaningPdfPageRecallCorpus.CORPUS_ID, report.fixtureCorpusId)
        assertEquals(3, report.caseCount)
        assertTrue(report.boostedHitsAt1 == report.caseCount)
        assertTrue(report.cosineOnlyHitsAt1 < report.caseCount)
        assertTrue(report.recommendsE4bForSemanticOnly)
        assertTrue(report.claims.all { it.hasMeasuredEvidence })
        assertFalse(report.claims.any { !it.mayPublishAsReleasePromise() })
        assertTrue(report.notes.contains("Does not authorize product AVAILABLE"))
        assertTrue(report.notes.contains("E4b"))
    }

    @Test
    fun mira_case_fails_cosine_only_and_passes_boost() {
        val mira = MeaningPdfPageRecallCorpus.cases().first { it.caseId == "mira-5page-p5" }
        assertFalse(
            MeasureMeaningPdfPageRecallBaseline.topMatchesExpected(
                mira,
                MeaningPdfPageRecallRankingMode.COSINE_ONLY,
            ),
        )
        assertTrue(
            MeasureMeaningPdfPageRecallBaseline.topMatchesExpected(
                mira,
                MeaningPdfPageRecallRankingMode.EVIDENCE_TOKEN_BOOSTED,
            ),
        )
    }
}
