package com.memora.app.work

import com.memora.app.application.documents.SafPdfFolderIndexingOutcome
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafPdfDiscoveryWorkDecisionMapperTest {
    @Test
    fun indexed_with_more_continues() {
        val decision = SafPdfDiscoveryWorkDecisionMapper.map(
            SafPdfFolderIndexingOutcome.Indexed(
                sourceId = SourceId("saf-pdf"),
                discoveredAssetCount = 3,
                hasMore = true,
            ),
        )
        assertEquals(SafPdfDiscoveryWorkDecision.Continue(pageAssetCount = 3), decision)
    }

    @Test
    fun indexed_without_more_completes() {
        val decision = SafPdfDiscoveryWorkDecisionMapper.map(
            SafPdfFolderIndexingOutcome.Indexed(
                sourceId = SourceId("saf-pdf"),
                discoveredAssetCount = 2,
                hasMore = false,
            ),
        )
        assertEquals(SafPdfDiscoveryWorkDecision.Completed(pageAssetCount = 2), decision)
    }

    @Test
    fun access_outcomes_stop() {
        assertEquals(
            SafPdfDiscoveryWorkDecision.AccessStopped,
            SafPdfDiscoveryWorkDecisionMapper.map(SafPdfFolderIndexingOutcome.AccessRevoked),
        )
        assertEquals(
            SafPdfDiscoveryWorkDecision.AccessStopped,
            SafPdfDiscoveryWorkDecisionMapper.map(SafPdfFolderIndexingOutcome.AccessRequired),
        )
        assertEquals(
            SafPdfDiscoveryWorkDecision.AccessStopped,
            SafPdfDiscoveryWorkDecisionMapper.map(SafPdfFolderIndexingOutcome.SourceNotConnected),
        )
    }

    @Test
    fun provider_failure_is_retryable() {
        val decision = SafPdfDiscoveryWorkDecisionMapper.map(
            SafPdfFolderIndexingOutcome.Failed(
                DiscoveryFailure(code = "provider", message = "Temporary provider error"),
            ),
        )
        assertTrue(decision is SafPdfDiscoveryWorkDecision.RetryableFailure)
    }
}
