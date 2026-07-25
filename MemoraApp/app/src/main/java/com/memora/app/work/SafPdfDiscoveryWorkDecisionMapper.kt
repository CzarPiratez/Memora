package com.memora.app.work

import com.memora.app.application.documents.SafPdfFolderIndexingOutcome

/**
 * Maps one SAF PDF discovery page outcome to the next WorkManager action.
 *
 * Pure; no Android WorkManager types. Never opens documents or schedules extract.
 */
internal object SafPdfDiscoveryWorkDecisionMapper {
    fun map(outcome: SafPdfFolderIndexingOutcome): SafPdfDiscoveryWorkDecision = when (outcome) {
        is SafPdfFolderIndexingOutcome.Indexed -> if (outcome.hasMore) {
            SafPdfDiscoveryWorkDecision.Continue(
                pageAssetCount = outcome.discoveredAssetCount,
            )
        } else {
            SafPdfDiscoveryWorkDecision.Completed(
                pageAssetCount = outcome.discoveredAssetCount,
            )
        }

        SafPdfFolderIndexingOutcome.AccessRequired,
        SafPdfFolderIndexingOutcome.AccessRevoked,
        SafPdfFolderIndexingOutcome.SourceNotConnected,
        -> SafPdfDiscoveryWorkDecision.AccessStopped

        is SafPdfFolderIndexingOutcome.Failed -> SafPdfDiscoveryWorkDecision.RetryableFailure(
            message = outcome.failure.message,
        )
    }
}

internal sealed interface SafPdfDiscoveryWorkDecision {
    data class Continue(val pageAssetCount: Int) : SafPdfDiscoveryWorkDecision {
        init {
            require(pageAssetCount >= 0)
        }
    }

    data class Completed(val pageAssetCount: Int) : SafPdfDiscoveryWorkDecision {
        init {
            require(pageAssetCount >= 0)
        }
    }

    data object AccessStopped : SafPdfDiscoveryWorkDecision

    data class RetryableFailure(val message: String) : SafPdfDiscoveryWorkDecision {
        init {
            require(message.isNotBlank())
        }
    }
}
