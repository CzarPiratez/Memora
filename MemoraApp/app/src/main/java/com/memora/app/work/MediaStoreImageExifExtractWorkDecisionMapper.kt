package com.memora.app.work

import com.memora.app.application.images.PendingImageExifExtractOutcome

/**
 * Maps one pending image EXIF outcome to the next WorkManager action.
 *
 * Pure; no Android WorkManager types.
 */
internal object MediaStoreImageExifExtractWorkDecisionMapper {
    fun map(outcome: PendingImageExifExtractOutcome): MediaStoreImageExifExtractWorkDecision =
        when (outcome) {
            PendingImageExifExtractOutcome.NoPending ->
                MediaStoreImageExifExtractWorkDecision.CompletedDrain

            is PendingImageExifExtractOutcome.Persisted -> if (outcome.hasMorePending) {
                MediaStoreImageExifExtractWorkDecision.Continue(
                    afterSourceAssetKey = outcome.processedSourceAssetKey,
                )
            } else {
                MediaStoreImageExifExtractWorkDecision.CompletedDrain
            }

            PendingImageExifExtractOutcome.AccessStopped ->
                MediaStoreImageExifExtractWorkDecision.AccessStopped

            PendingImageExifExtractOutcome.RetryableFailure,
            PendingImageExifExtractOutcome.Cancelled,
            -> MediaStoreImageExifExtractWorkDecision.RetryableFailure
        }
}

internal sealed interface MediaStoreImageExifExtractWorkDecision {
    data class Continue(val afterSourceAssetKey: String) : MediaStoreImageExifExtractWorkDecision {
        init {
            require(afterSourceAssetKey.isNotBlank())
        }
    }

    data object CompletedDrain : MediaStoreImageExifExtractWorkDecision

    data object AccessStopped : MediaStoreImageExifExtractWorkDecision

    data object RetryableFailure : MediaStoreImageExifExtractWorkDecision
}
