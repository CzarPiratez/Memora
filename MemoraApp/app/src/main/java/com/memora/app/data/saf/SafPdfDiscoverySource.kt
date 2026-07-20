package com.memora.app.data.saf

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAccessModel
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceCapability
import com.memora.app.domain.discovery.AssetDiscoverySource
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.SourceAccessState
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads one bounded metadata page from a user-approved SAF document tree.
 *
 * This adapter checks Android's retained read grant for every call. It only examines
 * immediate-child metadata and emits declared PDFs; it never opens a document URI or
 * reads PDF content. Descendant traversal is deliberately a later, documented step.
 */
class SafPdfDiscoverySource(
    private val approval: DocumentTreeApproval,
    private val catalog: SafDocumentTreeCatalog,
    private val clock: Clock = Clock.systemUTC(),
) : AssetDiscoverySource {
    override val capability: SourceCapability = SourceCapability(
        sourceId = approval.sourceId,
        supportedAssetTypes = setOf(AssetType.PDF),
        accessModel = SourceAccessModel.PERSISTED_DOCUMENT_ACCESS,
        supportsIncrementalDiscovery = true,
        supportsBackgroundIndexing = true,
    )

    override suspend fun accessState(): SourceAccessState = if (
        catalog.hasPersistedReadAccess(approval.treeUri)
    ) {
        SourceAccessState.GRANTED
    } else {
        SourceAccessState.ACCESS_REVOKED
    }

    override suspend fun discover(request: DiscoveryRequest): DiscoveryResult = withContext(Dispatchers.IO) {
        try {
            if (accessState() != SourceAccessState.GRANTED) {
                return@withContext DiscoveryResult.AccessRevoked
            }
            val checkpoint = checkpointFor(request)
            val metadataPage = catalog.readChildMetadataPage(
                treeUri = approval.treeUri,
                afterDocumentId = checkpoint.afterDocumentId,
                limit = request.batchSize,
            )
            val consumed = metadataPage.documents
            val nextCheckpoint = consumed.lastOrNull()?.documentId?.let { documentId ->
                SafPdfDiscoveryCheckpoint(approval.sourceId, documentId)
            } ?: checkpoint

            DiscoveryResult.Page(
                DiscoveryPage(
                    sourceId = approval.sourceId,
                    assets = consumed
                        .asSequence()
                        .filter(::isPdf)
                        .map { metadata -> metadata.toAsset(approval, Instant.now(clock)) }
                        .toList(),
                    checkpoint = nextCheckpoint.toCursor(),
                    hasMore = metadataPage.hasMore,
                )
            )
        } catch (_: SecurityException) {
            DiscoveryResult.AccessRevoked
        } catch (_: IllegalArgumentException) {
            DiscoveryResult.Failed(
                DiscoveryFailure(
                    code = "INVALID_SAF_CHECKPOINT",
                    message = "Memora could not safely resume the approved PDF folder scan.",
                )
            )
        } catch (_: Exception) {
            DiscoveryResult.Failed(
                DiscoveryFailure(
                    code = "SAF_DOCUMENT_QUERY_FAILED",
                    message = "Memora could not read PDF metadata from the approved folder. You can retry later.",
                )
            )
        }
    }

    private fun checkpointFor(request: DiscoveryRequest): SafPdfDiscoveryCheckpoint {
        val requested = request.cursor?.let(SafPdfDiscoveryCheckpoint::from)
            ?: return SafPdfDiscoveryCheckpoint.initial(approval.sourceId)
        require(requested.sourceId == approval.sourceId) {
            "A SAF discovery cursor must belong to the approved document tree."
        }
        return requested
    }

    private fun isPdf(metadata: SafDocumentMetadata): Boolean = metadata.mimeType.equals(
        PDF_MIME_TYPE,
        ignoreCase = true,
    )

    private fun SafDocumentMetadata.toAsset(
        approval: DocumentTreeApproval,
        discoveredAt: Instant,
    ): Asset {
        return Asset(
            identity = AssetIdentity(
                sourceId = approval.sourceId,
                sourceAssetKey = SourceAssetKey(documentId),
            ),
            type = AssetType.PDF,
            location = AssetLocation(documentUri),
            fingerprint = AssetFingerprint(
                "$documentId:${lastModifiedEpochMillis ?: UNKNOWN_VERSION}:${sizeBytes ?: UNKNOWN_SIZE}:$mimeType",
            ),
            discoveredAt = discoveredAt,
            displayName = displayName?.takeIf(String::isNotBlank),
            sourceModifiedAt = lastModifiedEpochMillis?.let(Instant::ofEpochMilli),
        )
    }

    private companion object {
        const val PDF_MIME_TYPE = "application/pdf"
        const val UNKNOWN_VERSION = "unknown-version"
        const val UNKNOWN_SIZE = "unknown-size"
    }
}
