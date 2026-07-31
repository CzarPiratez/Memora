package com.memora.app.work

import com.memora.app.application.images.PendingPhotoOcrExtractOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStorePhotoOcrExtractWorkDecisionMapperTest {
    @Test
    fun continuesOnlyWhenMorePhotosRemain() {
        assertEquals(
            MediaStorePhotoOcrExtractWorkDecision.Continue("photo-1"),
            MediaStorePhotoOcrExtractWorkDecisionMapper.map(
                PendingPhotoOcrExtractOutcome.Persisted("photo-1", true),
            ),
        )
        assertEquals(
            MediaStorePhotoOcrExtractWorkDecision.CompletedDrain,
            MediaStorePhotoOcrExtractWorkDecisionMapper.map(
                PendingPhotoOcrExtractOutcome.Persisted("photo-1", false),
            ),
        )
    }

    @Test
    fun accessStopsAndCancellationRetries() {
        assertEquals(
            MediaStorePhotoOcrExtractWorkDecision.AccessStopped,
            MediaStorePhotoOcrExtractWorkDecisionMapper.map(
                PendingPhotoOcrExtractOutcome.AccessStopped,
            ),
        )
        assertEquals(
            MediaStorePhotoOcrExtractWorkDecision.RetryableFailure,
            MediaStorePhotoOcrExtractWorkDecisionMapper.map(
                PendingPhotoOcrExtractOutcome.Cancelled,
            ),
        )
    }
}
