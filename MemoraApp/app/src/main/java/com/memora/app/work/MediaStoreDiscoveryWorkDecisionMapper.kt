package com.memora.app.work

import com.memora.app.application.discovery.MediaStoreIndexingOutcome

/**
 * Maps one MediaStore discovery page outcome to the next WorkManager action.
 *
 * Pure; no Android WorkManager types. Never opens image bytes or schedules OCR.
 */
internal object MediaStoreDiscoveryWorkDecisionMapper {
    fun map(outcome: MediaStoreIndexingOutcome): MediaStoreDiscoveryWorkDecision = when (outcome) {
        is MediaStoreIndexingOutcome.Indexed -> if (outcome.hasMore) {
            MediaStoreDiscoveryWorkDecision.Continue(
                pageAssetCount = outcome.discoveredAssetCount,
                accessScope = outcome.accessScope.name,
            )
        } else {
            MediaStoreDiscoveryWorkDecision.Completed(
                pageAssetCount = outcome.discoveredAssetCount,
                accessScope = outcome.accessScope.name,
            )
        }

        MediaStoreIndexingOutcome.AccessRequired,
        MediaStoreIndexingOutcome.AccessRevoked,
        -> MediaStoreDiscoveryWorkDecision.AccessStopped

        is MediaStoreIndexingOutcome.Failed -> MediaStoreDiscoveryWorkDecision.RetryableFailure(
            message = outcome.failure.message,
        )
    }
}

internal sealed interface MediaStoreDiscoveryWorkDecision {
    data class Continue(
        val pageAssetCount: Int,
        val accessScope: String,
    ) : MediaStoreDiscoveryWorkDecision {
        init {
            require(pageAssetCount >= 0)
            require(accessScope.isNotBlank())
        }
    }

    data class Completed(
        val pageAssetCount: Int,
        val accessScope: String,
    ) : MediaStoreDiscoveryWorkDecision {
        init {
            require(pageAssetCount >= 0)
            require(accessScope.isNotBlank())
        }
    }

    data object AccessStopped : MediaStoreDiscoveryWorkDecision

    data class RetryableFailure(val message: String) : MediaStoreDiscoveryWorkDecision {
        init {
            require(message.isNotBlank())
        }
    }
}
