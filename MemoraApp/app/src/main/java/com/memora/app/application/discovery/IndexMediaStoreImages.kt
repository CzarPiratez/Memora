package com.memora.app.application.discovery

import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import javax.inject.Inject

/** Application port used by presentation code to start one explicit image-library scan. */
interface MediaStoreImageIndexer {
    suspend operator fun invoke(): MediaStoreIndexingOutcome
}

/**
 * Runs exactly one explicitly requested, bounded MediaStore discovery page.
 *
 * It neither requests Android permission nor starts work by itself. A later ViewModel
 * may call it only after a deliberate user action and render the immutable outcome.
 */
class IndexMediaStoreImages @Inject constructor(
    private val imageLibrarySource: ImageLibraryDiscoverySource,
    private val discoverSourcePage: DiscoverSourcePage,
) : MediaStoreImageIndexer {
    override suspend operator fun invoke(): MediaStoreIndexingOutcome = invoke(
        batchSize = DiscoveryRequest.DEFAULT_BATCH_SIZE,
    )

    suspend operator fun invoke(
        batchSize: Int = DiscoveryRequest.DEFAULT_BATCH_SIZE,
    ): MediaStoreIndexingOutcome {
        val accessScope = imageLibrarySource.accessScope()
            ?: return MediaStoreIndexingOutcome.AccessRequired

        return when (val result = discoverSourcePage(imageLibrarySource, batchSize)) {
            is DiscoveryResult.Page -> MediaStoreIndexingOutcome.Indexed(
                discoveredAssetCount = result.value.assets.size,
                hasMore = result.value.hasMore,
                accessScope = accessScope,
            )

            DiscoveryResult.AccessRequired -> MediaStoreIndexingOutcome.AccessRequired
            DiscoveryResult.AccessRevoked -> MediaStoreIndexingOutcome.AccessRevoked
            is DiscoveryResult.Failed -> MediaStoreIndexingOutcome.Failed(result.failure)
        }
    }
}

/** Immutable outcome for a later ViewModel; no UI behavior is embedded here. */
sealed interface MediaStoreIndexingOutcome {
    data class Indexed(
        val discoveredAssetCount: Int,
        val hasMore: Boolean,
        val accessScope: ImageLibraryAccessScope,
    ) : MediaStoreIndexingOutcome {
        init {
            require(discoveredAssetCount >= 0) {
                "A MediaStore indexing outcome cannot contain a negative asset count."
            }
        }
    }

    data object AccessRequired : MediaStoreIndexingOutcome

    data object AccessRevoked : MediaStoreIndexingOutcome

    data class Failed(val failure: DiscoveryFailure) : MediaStoreIndexingOutcome
}
