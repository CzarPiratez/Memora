package com.memora.app.work

import com.memora.app.application.images.PendingImageExifExtractOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreImageExifExtractWorkDecisionMapperTest {
    @Test
    fun persisted_with_more_continues() {
        assertEquals(
            MediaStoreImageExifExtractWorkDecision.Continue(afterSourceAssetKey = "img-a"),
            MediaStoreImageExifExtractWorkDecisionMapper.map(
                PendingImageExifExtractOutcome.Persisted(
                    processedSourceAssetKey = "img-a",
                    hasMorePending = true,
                ),
            ),
        )
    }

    @Test
    fun persisted_without_more_completes() {
        assertEquals(
            MediaStoreImageExifExtractWorkDecision.CompletedDrain,
            MediaStoreImageExifExtractWorkDecisionMapper.map(
                PendingImageExifExtractOutcome.Persisted(
                    processedSourceAssetKey = "img-a",
                    hasMorePending = false,
                ),
            ),
        )
    }

    @Test
    fun no_pending_completes() {
        assertEquals(
            MediaStoreImageExifExtractWorkDecision.CompletedDrain,
            MediaStoreImageExifExtractWorkDecisionMapper.map(PendingImageExifExtractOutcome.NoPending),
        )
    }

    @Test
    fun access_stopped_fails() {
        assertEquals(
            MediaStoreImageExifExtractWorkDecision.AccessStopped,
            MediaStoreImageExifExtractWorkDecisionMapper.map(
                PendingImageExifExtractOutcome.AccessStopped,
            ),
        )
    }

    @Test
    fun retryable_maps_to_retry() {
        assertEquals(
            MediaStoreImageExifExtractWorkDecision.RetryableFailure,
            MediaStoreImageExifExtractWorkDecisionMapper.map(
                PendingImageExifExtractOutcome.RetryableFailure,
            ),
        )
    }
}
