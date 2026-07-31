package com.memora.app.work

import com.memora.app.application.images.PendingPhotoOcrExtractOutcome

internal sealed interface MediaStorePhotoOcrExtractWorkDecision {
    data class Continue(val afterSourceAssetKey: String) : MediaStorePhotoOcrExtractWorkDecision
    data object CompletedDrain : MediaStorePhotoOcrExtractWorkDecision
    data object AccessStopped : MediaStorePhotoOcrExtractWorkDecision
    data object RetryableFailure : MediaStorePhotoOcrExtractWorkDecision
}

internal object MediaStorePhotoOcrExtractWorkDecisionMapper {
    fun map(outcome: PendingPhotoOcrExtractOutcome): MediaStorePhotoOcrExtractWorkDecision =
        when (outcome) {
            PendingPhotoOcrExtractOutcome.NoPending ->
                MediaStorePhotoOcrExtractWorkDecision.CompletedDrain
            is PendingPhotoOcrExtractOutcome.Persisted -> if (outcome.hasMorePending) {
                MediaStorePhotoOcrExtractWorkDecision.Continue(outcome.processedSourceAssetKey)
            } else {
                MediaStorePhotoOcrExtractWorkDecision.CompletedDrain
            }
            PendingPhotoOcrExtractOutcome.AccessStopped ->
                MediaStorePhotoOcrExtractWorkDecision.AccessStopped
            PendingPhotoOcrExtractOutcome.RetryableFailure,
            PendingPhotoOcrExtractOutcome.Cancelled,
            -> MediaStorePhotoOcrExtractWorkDecision.RetryableFailure
        }
}
