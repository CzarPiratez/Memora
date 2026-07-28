package com.memora.app.work

import com.memora.app.application.images.PendingScreenshotOcrExtractOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreScreenshotOcrExtractWorkDecisionMapperTest {
    @Test
    fun persisted_with_more_continues() {
        assertEquals(
            MediaStoreScreenshotOcrExtractWorkDecision.Continue(afterSourceAssetKey = "shot-a"),
            MediaStoreScreenshotOcrExtractWorkDecisionMapper.map(
                PendingScreenshotOcrExtractOutcome.Persisted(
                    processedSourceAssetKey = "shot-a",
                    hasMorePending = true,
                ),
            ),
        )
    }

    @Test
    fun persisted_without_more_completes() {
        assertEquals(
            MediaStoreScreenshotOcrExtractWorkDecision.CompletedDrain,
            MediaStoreScreenshotOcrExtractWorkDecisionMapper.map(
                PendingScreenshotOcrExtractOutcome.Persisted(
                    processedSourceAssetKey = "shot-a",
                    hasMorePending = false,
                ),
            ),
        )
    }

    @Test
    fun no_pending_completes() {
        assertEquals(
            MediaStoreScreenshotOcrExtractWorkDecision.CompletedDrain,
            MediaStoreScreenshotOcrExtractWorkDecisionMapper.map(PendingScreenshotOcrExtractOutcome.NoPending),
        )
    }

    @Test
    fun access_stopped_fails() {
        assertEquals(
            MediaStoreScreenshotOcrExtractWorkDecision.AccessStopped,
            MediaStoreScreenshotOcrExtractWorkDecisionMapper.map(
                PendingScreenshotOcrExtractOutcome.AccessStopped,
            ),
        )
    }

    @Test
    fun retryable_and_cancelled_retry() {
        assertEquals(
            MediaStoreScreenshotOcrExtractWorkDecision.RetryableFailure,
            MediaStoreScreenshotOcrExtractWorkDecisionMapper.map(
                PendingScreenshotOcrExtractOutcome.RetryableFailure,
            ),
        )
        assertEquals(
            MediaStoreScreenshotOcrExtractWorkDecision.RetryableFailure,
            MediaStoreScreenshotOcrExtractWorkDecisionMapper.map(
                PendingScreenshotOcrExtractOutcome.Cancelled,
            ),
        )
    }
}
