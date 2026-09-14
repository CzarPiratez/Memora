package com.memora.app.work

import com.memora.app.application.intelligence.MeaningIndexDrainSessionResult

internal sealed interface MeaningIndexWorkDecision {
    data object Continue : MeaningIndexWorkDecision
    data object CompletedDrain : MeaningIndexWorkDecision
    data object StopAndReport : MeaningIndexWorkDecision
    data object RetryableFailure : MeaningIndexWorkDecision
}

/**
 * Maps one [MeaningIndexDrainSession] outcome onto WorkManager.
 *
 * Budget-exhausted is Continue (re-enqueue). Engine unavailable and selection
 * disagreement are StopAndReport — never retry, never Continue. Unexpected
 * exceptions are RetryableFailure.
 */
internal object MeaningIndexWorkDecisionMapper {
    fun map(result: MeaningIndexDrainSessionResult): MeaningIndexWorkDecision = when (result) {
        is MeaningIndexDrainSessionResult.BudgetExhausted -> MeaningIndexWorkDecision.Continue
        is MeaningIndexDrainSessionResult.Finished -> MeaningIndexWorkDecision.CompletedDrain
        is MeaningIndexDrainSessionResult.Stopped,
        is MeaningIndexDrainSessionResult.EngineUnavailable,
        is MeaningIndexDrainSessionResult.SelectionDisagreed,
        -> MeaningIndexWorkDecision.StopAndReport
        MeaningIndexDrainSessionResult.RetryableFailure ->
            MeaningIndexWorkDecision.RetryableFailure
    }
}
