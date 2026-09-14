package com.memora.app.work

import com.memora.app.application.intelligence.MeaningIndexDrainSessionResult
import org.junit.Assert.assertEquals
import org.junit.Test

class MeaningIndexWorkDecisionMapperTest {
    @Test
    fun budget_exhausted_continues() {
        assertEquals(
            MeaningIndexWorkDecision.Continue,
            MeaningIndexWorkDecisionMapper.map(
                MeaningIndexDrainSessionResult.BudgetExhausted(
                    indexedMemories = 50,
                    remainingPending = 200,
                ),
            ),
        )
    }

    @Test
    fun finished_completes_the_drain() {
        assertEquals(
            MeaningIndexWorkDecision.CompletedDrain,
            MeaningIndexWorkDecisionMapper.map(
                MeaningIndexDrainSessionResult.Finished(
                    indexedMemories = 12,
                    remainingPending = 0,
                ),
            ),
        )
    }

    @Test
    fun engine_unavailable_stops_and_does_not_retry() {
        assertEquals(
            MeaningIndexWorkDecision.StopAndReport,
            MeaningIndexWorkDecisionMapper.map(
                MeaningIndexDrainSessionResult.EngineUnavailable("model missing"),
            ),
        )
    }

    @Test
    fun selection_disagreement_stops_and_does_not_retry() {
        assertEquals(
            MeaningIndexWorkDecision.StopAndReport,
            MeaningIndexWorkDecisionMapper.map(
                MeaningIndexDrainSessionResult.SelectionDisagreed(4),
            ),
        )
    }

    @Test
    fun user_stop_does_not_retry_or_continue() {
        assertEquals(
            MeaningIndexWorkDecision.StopAndReport,
            MeaningIndexWorkDecisionMapper.map(
                MeaningIndexDrainSessionResult.Stopped(
                    indexedMemories = 25,
                    remainingPending = 40,
                ),
            ),
        )
    }

    @Test
    fun unexpected_failure_retries() {
        assertEquals(
            MeaningIndexWorkDecision.RetryableFailure,
            MeaningIndexWorkDecisionMapper.map(MeaningIndexDrainSessionResult.RetryableFailure),
        )
    }
}
