package com.memora.app.work

import com.memora.app.application.notes.PendingOneNotePageExtractOutcome

internal sealed interface OneNotePageExtractWorkDecision {
    data class Continue(val afterSourceAssetKey: String) : OneNotePageExtractWorkDecision

    data object CompletedDrain : OneNotePageExtractWorkDecision

    data object AccessStopped : OneNotePageExtractWorkDecision

    data object RetryableFailure : OneNotePageExtractWorkDecision
}

internal object OneNotePageExtractWorkDecisionMapper {
    fun map(outcome: PendingOneNotePageExtractOutcome): OneNotePageExtractWorkDecision =
        when (outcome) {
            PendingOneNotePageExtractOutcome.NoPending ->
                OneNotePageExtractWorkDecision.CompletedDrain

            is PendingOneNotePageExtractOutcome.Persisted -> if (outcome.hasMorePending) {
                OneNotePageExtractWorkDecision.Continue(
                    afterSourceAssetKey = outcome.processedSourceAssetKey,
                )
            } else {
                OneNotePageExtractWorkDecision.CompletedDrain
            }

            PendingOneNotePageExtractOutcome.AccessStopped ->
                OneNotePageExtractWorkDecision.AccessStopped

            PendingOneNotePageExtractOutcome.RetryableFailure,
            PendingOneNotePageExtractOutcome.Cancelled,
            -> OneNotePageExtractWorkDecision.RetryableFailure
        }
}
