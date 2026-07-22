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
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
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
 * one bounded immediate-child metadata page per invocation and emits declared PDFs.
 * Its source-owned checkpoint resumes a depth-first traversal across descendant
 * folders. It never opens a document URI or reads PDF content.
 */
class SafPdfDiscoverySource(
    private val approval: DocumentTreeApproval,
    private val accessValidator: DocumentTreeAccessValidator,
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

    override suspend fun accessState(): SourceAccessState = accessValidator.accessState(approval)

    override suspend fun discover(request: DiscoveryRequest): DiscoveryResult = withContext(Dispatchers.IO) {
        try {
            if (accessState() != SourceAccessState.GRANTED) {
                return@withContext DiscoveryResult.AccessRevoked
            }
            val checkpoint = checkpointFor(request)
            val currentFolder = checkpoint.frames.lastOrNull()
                ?: return@withContext completedPage(checkpoint)
            val metadataPage = catalog.readChildMetadataPage(
                treeUri = approval.treeUri,
                parentDocumentId = currentFolder.parentDocumentId,
                afterDocumentId = currentFolder.afterDocumentId,
                limit = request.batchSize,
            )
            val consumed = metadataPage.documents
            if (consumed.isEmpty() && metadataPage.hasMore) {
                return@withContext DiscoveryResult.Failed(
                    DiscoveryFailure(
                        code = "SAF_DOCUMENT_CURSOR_STALLED",
                        message = "Memora could not safely continue the approved PDF folder scan. You can retry later.",
                    )
                )
            }
            val nextCheckpoint = checkpoint.advance(currentFolder, metadataPage)

            DiscoveryResult.Page(
                DiscoveryPage(
                    sourceId = approval.sourceId,
                    assets = consumed
                        .asSequence()
                        .filter(::isPdf)
                        .map { metadata -> metadata.toAsset(approval, Instant.now(clock)) }
                        .toList(),
                    checkpoint = nextCheckpoint.toCursor(),
                    hasMore = nextCheckpoint.frames.isNotEmpty(),
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

    private fun completedPage(checkpoint: SafPdfDiscoveryCheckpoint): DiscoveryResult.Page = DiscoveryResult.Page(
        DiscoveryPage(
            sourceId = approval.sourceId,
            assets = emptyList(),
            checkpoint = checkpoint.toCursor(),
            hasMore = false,
        )
    )

    private fun SafPdfDiscoveryCheckpoint.advance(
        currentFolder: SafPdfDiscoveryCheckpoint.FolderFrame,
        metadataPage: SafDocumentTreeMetadataPage,
    ): SafPdfDiscoveryCheckpoint {
        val remaining = frames.dropLast(1).toMutableList()
        if (metadataPage.hasMore) {
            val lastDocumentId = metadataPage.documents.last().documentId
            remaining += currentFolder.copy(afterDocumentId = lastDocumentId)
        }
        metadataPage.documents
            .asReversed()
            .filter(::isFolder)
            .forEach { folder ->
                remaining += SafPdfDiscoveryCheckpoint.FolderFrame(
                    parentDocumentId = folder.documentId,
                    afterDocumentId = null,
                )
            }
        return SafPdfDiscoveryCheckpoint(approval.sourceId, remaining)
    }

    private fun isPdf(metadata: SafDocumentMetadata): Boolean = metadata.mimeType.equals(
        PDF_MIME_TYPE,
        ignoreCase = true,
    )

    private fun isFolder(metadata: SafDocumentMetadata): Boolean = metadata.mimeType == DIRECTORY_MIME_TYPE

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
        const val DIRECTORY_MIME_TYPE = "vnd.android.document/directory"
        const val UNKNOWN_VERSION = "unknown-version"
        const val UNKNOWN_SIZE = "unknown-size"
    }
}
