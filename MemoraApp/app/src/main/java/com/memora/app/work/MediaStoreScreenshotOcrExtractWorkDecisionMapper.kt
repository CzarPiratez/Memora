package com.memora.app.work

import com.memora.app.application.images.PendingScreenshotOcrExtractOutcome

internal sealed interface MediaStoreScreenshotOcrExtractWorkDecision {
    data class Continue(val afterSourceAssetKey: String) : MediaStoreScreenshotOcrExtractWorkDecision

    data object CompletedDrain : MediaStoreScreenshotOcrExtractWorkDecision

    data object AccessStopped : MediaStoreScreenshotOcrExtractWorkDecision

    data object RetryableFailure : MediaStoreScreenshotOcrExtractWorkDecision
}

internal object MediaStoreScreenshotOcrExtractWorkDecisionMapper {
    fun map(outcome: PendingScreenshotOcrExtractOutcome): MediaStoreScreenshotOcrExtractWorkDecision =
        when (outcome) {
            PendingScreenshotOcrExtractOutcome.NoPending ->
                MediaStoreScreenshotOcrExtractWorkDecision.CompletedDrain

            is PendingScreenshotOcrExtractOutcome.Persisted -> if (outcome.hasMorePending) {
                MediaStoreScreenshotOcrExtractWorkDecision.Continue(
                    afterSourceAssetKey = outcome.processedSourceAssetKey,
                )
            } else {
                MediaStoreScreenshotOcrExtractWorkDecision.CompletedDrain
            }

            PendingScreenshotOcrExtractOutcome.AccessStopped ->
                MediaStoreScreenshotOcrExtractWorkDecision.AccessStopped

            PendingScreenshotOcrExtractOutcome.RetryableFailure,
            PendingScreenshotOcrExtractOutcome.Cancelled,
            -> MediaStoreScreenshotOcrExtractWorkDecision.RetryableFailure
        }
}
