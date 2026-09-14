package com.memora.app.application.handoff

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves a read-only content URI for a user-tapped handoff.
 *
 * Shared by the share sheet and Open in another app: both need the same stored
 * URI the Open preview already read. Search is unchanged. UNFYND does not copy,
 * upload, or edit the original. Notes have no local file in this slice.
 */
@Singleton
class PrepareOriginalHandoff @Inject constructor(
    private val assetRepository: AssetRepository,
    private val imageLibraryDiscoverySource: ImageLibraryDiscoverySource,
    private val uriCandidates: HandoffImageUriCandidates,
    private val readable: ReadableContentUri,
    private val pdfUriAccess: HandoffPdfUriAccess,
) {
    suspend operator fun invoke(request: OriginalHandoffRequest): PreparedOriginalHandoff =
        withContext(Dispatchers.IO) {
            try {
                when (request) {
                    is OriginalHandoffRequest.Pdf -> preparePdf(request)
                    is OriginalHandoffRequest.Photo -> prepareImage(
                        request = request,
                        expectedType = AssetType.PHOTO,
                        mimeFallback = OriginalHandoffMime.PHOTO_FALLBACK,
                    )
                    is OriginalHandoffRequest.Screenshot -> prepareImage(
                        request = request,
                        expectedType = AssetType.SCREENSHOT,
                        mimeFallback = OriginalHandoffMime.SCREENSHOT_FALLBACK,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                PreparedOriginalHandoff.CouldNotHandOff
            }
        }

    private suspend fun preparePdf(
        request: OriginalHandoffRequest.Pdf,
    ): PreparedOriginalHandoff {
        val asset = findAsset(request.sourceId, request.sourceAssetKey)
            ?: return PreparedOriginalHandoff.SourceUnavailable
        if (asset.type != AssetType.PDF) return PreparedOriginalHandoff.CouldNotHandOff
        return when (
            val resolved = pdfUriAccess.resolve(request.sourceId, request.sourceAssetKey)
        ) {
            is HandoffPdfUri.Ready -> PreparedOriginalHandoff.Ready(
                uri = resolved.uri,
                mimeType = OriginalHandoffMime.PDF,
                label = request.label,
            )
            HandoffPdfUri.SourceUnavailable -> PreparedOriginalHandoff.SourceUnavailable
            HandoffPdfUri.CouldNotHandOff -> PreparedOriginalHandoff.CouldNotHandOff
        }
    }

    private suspend fun prepareImage(
        request: OriginalHandoffRequest,
        expectedType: AssetType,
        mimeFallback: String,
    ): PreparedOriginalHandoff {
        if (imageLibraryDiscoverySource.accessScope() == null) {
            return PreparedOriginalHandoff.SourceUnavailable
        }
        val asset = findAsset(request.sourceId, request.sourceAssetKey)
            ?: return PreparedOriginalHandoff.SourceUnavailable
        if (asset.type != expectedType) return PreparedOriginalHandoff.CouldNotHandOff
        val storedUri = asset.location.value.trim()
        if (storedUri.isEmpty()) return PreparedOriginalHandoff.CouldNotHandOff
        val displayName = asset.displayName?.takeIf { it.isNotBlank() } ?: request.label
        val candidates = uriCandidates.candidates(storedUri = storedUri, displayName = displayName)
        for (uri in candidates) {
            if (uri.isBlank() || !readable.canRead(uri)) continue
            return PreparedOriginalHandoff.Ready(
                uri = uri,
                mimeType = readable.mimeType(uri, mimeFallback),
                label = request.label,
            )
        }
        return PreparedOriginalHandoff.SourceUnavailable
    }

    private suspend fun findAsset(sourceId: String, sourceAssetKey: String): Asset? =
        assetRepository.find(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        )?.asset
}
