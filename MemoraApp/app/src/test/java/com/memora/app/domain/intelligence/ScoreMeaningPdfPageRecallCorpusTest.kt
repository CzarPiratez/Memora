package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreMeaningPdfPageRecallCorpusTest {
    @Test
    fun scores_labeled_texts_to_finite_cosines() {
        val engine = ConstantEmbeddingEngine()
        val scored = ScoreMeaningPdfPageRecallCorpus.score(engine)
        assertEquals(3, scored.scoredCases.size)
        assertTrue(scored.embedCount >= 3)
        assertTrue(
            scored.scoredCases
                .flatMap { it.candidates }
                .all { it.injectedCosine.isFinite() && it.injectedCosine in 0f..1f },
        )
        assertEquals(ConstantEmbeddingEngine.MODEL, scored.model)
    }

    @Test
    fun on_device_measure_records_live_path_without_boost_bar() {
        val scoreResult = ScoreMeaningPdfPageRecallResult(
            scoredCases = MeaningPdfPageRecallCorpus.cases(),
            model = ConstantEmbeddingEngine.MODEL,
            embedCount = 12,
            embedWallMs = 42L,
        )
        val report = MeasureOnDeviceMeaningPdfPageRecallBaseline.measure(
            scoreResult = scoreResult,
            deviceTierId = MeaningPdfPageRecallCorpus.DEVICE_TIER_JVM_UNIT,
        )
        assertEquals(MeaningPdfPageRecallCorpus.CORPUS_ID, report.baseline.fixtureCorpusId)
        assertTrue(report.baseline.cosineOnlyHitsAt1 < report.baseline.caseCount)
        assertEquals(report.baseline.caseCount, report.baseline.boostedHitsAt1)
        assertTrue(report.baseline.recommendsE4bForSemanticOnly)
        assertTrue(report.baseline.notes.startsWith("M2 on-device harness"))
        assertTrue(report.baseline.notes.contains("Does not authorize product AVAILABLE"))
        assertEquals(42L, report.embedWallMs)
        assertEquals(12, report.embedCount)
    }
}

private class ConstantEmbeddingEngine : EmbeddingEngine {
    override fun availability(): CapabilityAvailability =
        CapabilityAvailability.Available(MODEL)

    override fun limits(): CapabilityLimits? =
        CapabilityLimits(maxInputBytes = 32_768L, maxOutputItems = 1)

    override fun embedText(text: String): EmbeddingEncodeResult {
        require(text.isNotBlank())
        return EmbeddingEncodeResult.Success(
            vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f)),
            model = MODEL,
        )
    }

    companion object {
        val MODEL = ModelVersionIdentity("fake-constant-embedder", "test-1")
    }
}
