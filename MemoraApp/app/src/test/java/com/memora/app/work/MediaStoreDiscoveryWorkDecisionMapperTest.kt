package com.memora.app.work

import com.memora.app.application.discovery.MediaStoreIndexingOutcome
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreDiscoveryWorkDecisionMapperTest {
    @Test
    fun indexed_with_more_continues() {
        val decision = MediaStoreDiscoveryWorkDecisionMapper.map(
            MediaStoreIndexingOutcome.Indexed(
                discoveredAssetCount = 3,
                hasMore = true,
                accessScope = ImageLibraryAccessScope.FULL_LIBRARY,
            ),
        )
        assertEquals(
            MediaStoreDiscoveryWorkDecision.Continue(
                pageAssetCount = 3,
                accessScope = "FULL_LIBRARY",
            ),
            decision,
        )
    }

    @Test
    fun indexed_without_more_completes() {
        val decision = MediaStoreDiscoveryWorkDecisionMapper.map(
            MediaStoreIndexingOutcome.Indexed(
                discoveredAssetCount = 2,
                hasMore = false,
                accessScope = ImageLibraryAccessScope.SELECTED_PHOTOS,
            ),
        )
        assertEquals(
            MediaStoreDiscoveryWorkDecision.Completed(
                pageAssetCount = 2,
                accessScope = "SELECTED_PHOTOS",
            ),
            decision,
        )
    }

    @Test
    fun access_outcomes_stop() {
        assertEquals(
            MediaStoreDiscoveryWorkDecision.AccessStopped,
            MediaStoreDiscoveryWorkDecisionMapper.map(MediaStoreIndexingOutcome.AccessRevoked),
        )
        assertEquals(
            MediaStoreDiscoveryWorkDecision.AccessStopped,
            MediaStoreDiscoveryWorkDecisionMapper.map(MediaStoreIndexingOutcome.AccessRequired),
        )
    }

    @Test
    fun provider_failure_is_retryable() {
        val decision = MediaStoreDiscoveryWorkDecisionMapper.map(
            MediaStoreIndexingOutcome.Failed(
                DiscoveryFailure(code = "busy", message = "Temporary catalogue error"),
            ),
        )
        assertTrue(decision is MediaStoreDiscoveryWorkDecision.RetryableFailure)
    }
}
