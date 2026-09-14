package com.memora.app.application.share

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
 * Resolves a read-only content URI for a user-tapped share sheet.
 *
 * Search is unchanged. UNFYND does not copy, upload, or edit the original.
 * Notes have no local file in this slice.
 */
@Singleton
class PrepareShareOriginal @Inject constructor(
    private val assetRepository: AssetRepository,
    private val imageLibraryDiscoverySource: ImageLibraryDiscoverySource,
    private val uriCandidates: ShareImageUriCandidates,
    private val readable: ReadableContentUri,
    private val pdfUriAccess: ShareablePdfUriAccess,
) {
    suspend operator fun invoke(request: ShareOriginalRequest): PreparedShareOriginal =
        withContext(Dispatchers.IO) {
            try {
                when (request) {
                    is ShareOriginalRequest.Pdf -> preparePdf(request)
                    is ShareOriginalRequest.Photo -> prepareImage(
                        request = request,
                        expectedType = AssetType.PHOTO,
                        mimeFallback = ShareOriginalMime.PHOTO_FALLBACK,
                    )
                    is ShareOriginalRequest.Screenshot -> prepareImage(
                        request = request,
                        expectedType = AssetType.SCREENSHOT,
                        mimeFallback = ShareOriginalMime.SCREENSHOT_FALLBACK,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                PreparedShareOriginal.CouldNotShare
            }
        }

    private suspend fun preparePdf(request: ShareOriginalRequest.Pdf): PreparedShareOriginal {
        val asset = findAsset(request.sourceId, request.sourceAssetKey)
            ?: return PreparedShareOriginal.SourceUnavailable
        if (asset.type != AssetType.PDF) return PreparedShareOriginal.CouldNotShare
        return when (
            val resolved = pdfUriAccess.resolve(request.sourceId, request.sourceAssetKey)
        ) {
            is ShareablePdfUri.Ready -> PreparedShareOriginal.Ready(
                uri = resolved.uri,
                mimeType = ShareOriginalMime.PDF,
                label = request.label,
            )
            ShareablePdfUri.SourceUnavailable -> PreparedShareOriginal.SourceUnavailable
            ShareablePdfUri.CouldNotShare -> PreparedShareOriginal.CouldNotShare
        }
    }

    private suspend fun prepareImage(
        request: ShareOriginalRequest,
        expectedType: AssetType,
        mimeFallback: String,
    ): PreparedShareOriginal {
        if (imageLibraryDiscoverySource.accessScope() == null) {
            return PreparedShareOriginal.SourceUnavailable
        }
        val asset = findAsset(request.sourceId, request.sourceAssetKey)
            ?: return PreparedShareOriginal.SourceUnavailable
        if (asset.type != expectedType) return PreparedShareOriginal.CouldNotShare
        val storedUri = asset.location.value.trim()
        if (storedUri.isEmpty()) return PreparedShareOriginal.CouldNotShare
        val displayName = asset.displayName?.takeIf { it.isNotBlank() } ?: request.label
        val candidates = uriCandidates.candidates(storedUri = storedUri, displayName = displayName)
        for (uri in candidates) {
            if (uri.isBlank() || !readable.canRead(uri)) continue
            return PreparedShareOriginal.Ready(
                uri = uri,
                mimeType = readable.mimeType(uri, mimeFallback),
                label = request.label,
            )
        }
        return PreparedShareOriginal.SourceUnavailable
    }

    private suspend fun findAsset(sourceId: String, sourceAssetKey: String): Asset? =
        assetRepository.find(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        )?.asset
}
