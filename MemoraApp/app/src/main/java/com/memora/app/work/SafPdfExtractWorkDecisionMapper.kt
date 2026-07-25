package com.memora.app.work

import com.memora.app.application.documents.PendingPdfLocalReadingOutcome

/**
 * Maps one pending PDF local-reading outcome to the next WorkManager action.
 *
 * Pure; no Android WorkManager types. Never opens documents.
 */
internal object SafPdfExtractWorkDecisionMapper {
    fun map(outcome: PendingPdfLocalReadingOutcome): SafPdfExtractWorkDecision = when (outcome) {
        PendingPdfLocalReadingOutcome.NoPending -> SafPdfExtractWorkDecision.CompletedDrain

        is PendingPdfLocalReadingOutcome.Persisted -> if (outcome.hasMorePending) {
            SafPdfExtractWorkDecision.Continue(afterSourceAssetKey = outcome.processedSourceAssetKey)
        } else {
            SafPdfExtractWorkDecision.CompletedDrain
        }

        is PendingPdfLocalReadingOutcome.SkippedPassword -> if (outcome.hasMorePending) {
            SafPdfExtractWorkDecision.Continue(afterSourceAssetKey = outcome.processedSourceAssetKey)
        } else {
            SafPdfExtractWorkDecision.CompletedDrain
        }

        PendingPdfLocalReadingOutcome.AccessStopped -> SafPdfExtractWorkDecision.AccessStopped

        PendingPdfLocalReadingOutcome.RetryableFailure,
        PendingPdfLocalReadingOutcome.Cancelled,
        -> SafPdfExtractWorkDecision.RetryableFailure
    }
}

internal sealed interface SafPdfExtractWorkDecision {
    data class Continue(val afterSourceAssetKey: String) : SafPdfExtractWorkDecision {
        init {
            require(afterSourceAssetKey.isNotBlank())
        }
    }

    data object CompletedDrain : SafPdfExtractWorkDecision

    data object AccessStopped : SafPdfExtractWorkDecision

    data object RetryableFailure : SafPdfExtractWorkDecision
}
