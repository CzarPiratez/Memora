package com.memora.app.application.documents

import com.memora.app.application.discovery.DiscoverSourcePage
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCheckpointRepository
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.discovery.PdfFolderDiscoverySourceFactory
import javax.inject.Inject

/** Application port for one explicit, bounded scan of one already approved PDF folder. */
interface SafPdfFolderIndexer {
    suspend operator fun invoke(sourceId: SourceId): SafPdfFolderIndexingOutcome
}

/**
 * Persists one bounded SAF PDF metadata page and its source-owned checkpoint.
 *
 * It never launches Android's picker, requests access, opens PDF bytes, extracts text,
 * copies a document, schedules background work, or decides which folder to scan.
 */
class IndexSafPdfFolder @Inject constructor(
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val sourceFactory: PdfFolderDiscoverySourceFactory,
    private val discoverSourcePage: DiscoverSourcePage,
    private val checkpointRepository: DiscoveryCheckpointRepository,
) : SafPdfFolderIndexer {
    override suspend operator fun invoke(sourceId: SourceId): SafPdfFolderIndexingOutcome = invoke(
        sourceId = sourceId,
        batchSize = DiscoveryRequest.DEFAULT_BATCH_SIZE,
    )

    suspend operator fun invoke(
        sourceId: SourceId,
        batchSize: Int = DiscoveryRequest.DEFAULT_BATCH_SIZE,
    ): SafPdfFolderIndexingOutcome {
        val approval = approvalRepository.find(sourceId)
            ?: return SafPdfFolderIndexingOutcome.SourceNotConnected
        clearCompletedSafPdfCheckpointIfNeeded(sourceId)
        val source = sourceFactory.create(approval)

        return when (val result = discoverSourcePage(source, batchSize)) {
            is DiscoveryResult.Page -> SafPdfFolderIndexingOutcome.Indexed(
                sourceId = sourceId,
                discoveredAssetCount = result.value.assets.size,
                hasMore = result.value.hasMore,
            )

            DiscoveryResult.AccessRequired -> SafPdfFolderIndexingOutcome.AccessRequired
            DiscoveryResult.AccessRevoked -> SafPdfFolderIndexingOutcome.AccessRevoked
            is DiscoveryResult.Failed -> SafPdfFolderIndexingOutcome.Failed(result.failure)
        }
    }

    /**
     * A completed SAF walk stores an empty frame list. Without clearing it, the next
     * explicit index request would return an empty page immediately and miss new PDFs.
     */
    private suspend fun clearCompletedSafPdfCheckpointIfNeeded(sourceId: SourceId) {
        if (checkpointRepository.isCheckpointCompleted(sourceId)) {
            checkpointRepository.delete(sourceId)
        }
    }
}

/** Immutable outcome for a later ViewModel; no UI behaviour is embedded here. */
sealed interface SafPdfFolderIndexingOutcome {
    data class Indexed(
        val sourceId: SourceId,
        val discoveredAssetCount: Int,
        val hasMore: Boolean,
    ) : SafPdfFolderIndexingOutcome {
        init {
            require(discoveredAssetCount >= 0) {
                "A SAF PDF indexing outcome cannot contain a negative asset count."
            }
        }
    }

    data object SourceNotConnected : SafPdfFolderIndexingOutcome

    data object AccessRequired : SafPdfFolderIndexingOutcome

    data object AccessRevoked : SafPdfFolderIndexingOutcome

    data class Failed(val failure: DiscoveryFailure) : SafPdfFolderIndexingOutcome
}
