package com.memora.app.work

import com.memora.app.application.documents.PendingPdfLocalReadingOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class SafPdfExtractWorkDecisionMapperTest {
    @Test
    fun persisted_with_more_continues() {
        assertEquals(
            SafPdfExtractWorkDecision.Continue(afterSourceAssetKey = "doc-a"),
            SafPdfExtractWorkDecisionMapper.map(
                PendingPdfLocalReadingOutcome.Persisted(
                    processedSourceAssetKey = "doc-a",
                    hasMorePending = true,
                ),
            ),
        )
    }

    @Test
    fun persisted_without_more_completes() {
        assertEquals(
            SafPdfExtractWorkDecision.CompletedDrain,
            SafPdfExtractWorkDecisionMapper.map(
                PendingPdfLocalReadingOutcome.Persisted(
                    processedSourceAssetKey = "doc-a",
                    hasMorePending = false,
                ),
            ),
        )
    }

    @Test
    fun password_skip_continues_when_more_remain() {
        assertEquals(
            SafPdfExtractWorkDecision.Continue(afterSourceAssetKey = "locked"),
            SafPdfExtractWorkDecisionMapper.map(
                PendingPdfLocalReadingOutcome.SkippedPassword(
                    processedSourceAssetKey = "locked",
                    hasMorePending = true,
                ),
            ),
        )
    }

    @Test
    fun no_pending_completes() {
        assertEquals(
            SafPdfExtractWorkDecision.CompletedDrain,
            SafPdfExtractWorkDecisionMapper.map(PendingPdfLocalReadingOutcome.NoPending),
        )
    }

    @Test
    fun access_stopped_fails() {
        assertEquals(
            SafPdfExtractWorkDecision.AccessStopped,
            SafPdfExtractWorkDecisionMapper.map(PendingPdfLocalReadingOutcome.AccessStopped),
        )
    }

    @Test
    fun retryable_maps_to_retry() {
        assertEquals(
            SafPdfExtractWorkDecision.RetryableFailure,
            SafPdfExtractWorkDecisionMapper.map(PendingPdfLocalReadingOutcome.RetryableFailure),
        )
    }
}
