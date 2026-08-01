package com.memora.app.application.notes

import com.memora.app.application.discovery.DiscoverSourcePage
import com.memora.app.data.notes.OneNotePagesDiscoverySource
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import javax.inject.Inject

/** Application port for one explicit, bounded OneNote page discovery. */
interface OneNotePagesIndexer {
    suspend operator fun invoke(
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): OneNoteDiscoveryOutcome

    companion object {
        const val DEFAULT_BATCH_SIZE: Int = 25
    }
}

/**
 * Runs exactly one user-started OneNote discovery page into NOTE Asset placeholders.
 * Does not extract page text (N4) and does not search notes (N5).
 */
class IndexOneNotePages @Inject constructor(
    private val oneNoteSource: OneNotePagesDiscoverySource,
    private val discoverSourcePage: DiscoverSourcePage,
    private val assetRepository: AssetRepository,
    private val oneNoteAuth: OneNoteInteractiveAuth,
) : OneNotePagesIndexer {
    override suspend fun invoke(batchSize: Int): OneNoteDiscoveryOutcome {
        if (oneNoteAuth.ensureSession() == null) {
            return OneNoteDiscoveryOutcome.AccessRequired
        }
        return when (
            val result = discoverSourcePage(
                source = oneNoteSource,
                batchSize = batchSize.coerceIn(
                    DiscoveryRequest.MIN_BATCH_SIZE,
                    DiscoveryRequest.MAX_BATCH_SIZE,
                ),
            )
        ) {
            is DiscoveryResult.Page -> OneNoteDiscoveryOutcome.Discovered(
                pageAssetCount = result.value.assets.size,
                totalNoteAssets = assetRepository.countBySourceAndType(
                    sourceId = OneNotePagesDiscoverySource.SOURCE_ID,
                    type = AssetType.NOTE,
                ),
                hasMore = result.value.hasMore,
            )
            DiscoveryResult.AccessRequired -> OneNoteDiscoveryOutcome.AccessRequired
            DiscoveryResult.AccessRevoked -> OneNoteDiscoveryOutcome.AccessRevoked
            is DiscoveryResult.Failed -> OneNoteDiscoveryOutcome.Failed(result.failure)
        }
    }
}

sealed interface OneNoteDiscoveryOutcome {
    data class Discovered(
        val pageAssetCount: Int,
        val totalNoteAssets: Int,
        val hasMore: Boolean,
    ) : OneNoteDiscoveryOutcome {
        init {
            require(pageAssetCount >= 0) { "pageAssetCount cannot be negative." }
            require(totalNoteAssets >= 0) { "totalNoteAssets cannot be negative." }
        }
    }

    data object AccessRequired : OneNoteDiscoveryOutcome

    data object AccessRevoked : OneNoteDiscoveryOutcome

    data class Failed(val failure: DiscoveryFailure) : OneNoteDiscoveryOutcome
}
