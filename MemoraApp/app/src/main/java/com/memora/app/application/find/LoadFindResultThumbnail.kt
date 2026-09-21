package com.memora.app.application.find

import android.os.CancellationSignal
import com.memora.app.application.documents.PdfPagePreviewRenderResult
import com.memora.app.application.documents.PdfPagePreviewRenderer
import com.memora.app.application.documents.PdfReadOnlyDescriptorAccess
import com.memora.app.application.documents.PdfReadOnlyDescriptorOutcome
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Read-only Find-card thumbnail. Does not rank, search, or replace Canonical Recall.
 *
 * Query answering stays on stored Memory evidence (LOCAL_AI §3 / P-11). This
 * reopens the original with the same grant Open uses, at list size, so the
 * person can recognise the file. A failed thumbnail never drops the hit.
 */
@Singleton
class LoadFindResultThumbnail @Inject constructor(
    private val cache: FindThumbnailCache,
    private val assetRepository: AssetRepository,
    private val imageLibraryDiscoverySource: ImageLibraryDiscoverySource,
    private val uriResolver: MediaStoreImageUriResolver,
    private val imageThumbnailLoader: ImageThumbnailLoader,
    private val descriptorAccess: PdfReadOnlyDescriptorAccess,
    private val pagePreviewRenderer: PdfPagePreviewRenderer,
) {
    private val decodeGate = Semaphore(FindThumbnailLimits.DECODE_PERMITS)

    suspend operator fun invoke(request: FindThumbnailRequest): FindThumbnailResult {
        cache.get(request.cacheKey)?.let { return it }
        return decodeGate.withPermit {
            coroutineContext.ensureActive()
            cache.get(request.cacheKey)?.let { return@withPermit it }
            val loaded = try {
                loadUncached(request)
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE)
            }
            cache.put(request.cacheKey, loaded)
            loaded
        }
    }

    private suspend fun loadUncached(request: FindThumbnailRequest): FindThumbnailResult =
        withContext(Dispatchers.IO) {
            when (request.assetType) {
                AssetType.NOTE -> FindThumbnailResult.Glyph(FindThumbnailGlyph.NOTE)
                AssetType.PHOTO, AssetType.SCREENSHOT -> loadImage(request)
                AssetType.PDF -> loadPdf(request)
            }
        }

    private suspend fun loadImage(request: FindThumbnailRequest): FindThumbnailResult {
        if (imageLibraryDiscoverySource.accessScope() == null) {
            return FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE)
        }
        val asset = assetRepository.find(
            AssetIdentity(SourceId(request.sourceId), SourceAssetKey(request.sourceAssetKey)),
        )?.asset ?: return FindThumbnailResult.Glyph(FindThumbnailGlyph.SOURCE_UNREACHABLE)
        if (asset.type != request.assetType) {
            return FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE)
        }
        val displayName = asset.displayName?.takeIf { it.isNotBlank() } ?: request.label
        val locations = uriResolver.candidateLocations(
            storedLocation = asset.location.value,
            displayName = displayName,
        )
        if (locations.isEmpty()) {
            return FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE)
        }
        var decodeFlake = false
        for (location in locations) {
            coroutineContext.ensureActive()
            when (val loaded = imageThumbnailLoader.load(location, FindThumbnailLimits.MAX_EDGE_PX)) {
                is ImageThumbnailLoad.Ready -> return loaded.toFindResult()
                ImageThumbnailLoad.CouldNotDecode -> decodeFlake = true
                ImageThumbnailLoad.Unreachable -> Unit
            }
        }
        return FindThumbnailResult.Glyph(FindThumbnailPolicy.imageMissGlyph(decodeFlake))
    }

    private suspend fun loadPdf(request: FindThumbnailRequest): FindThumbnailResult {
        val pageNumber = FindThumbnailPolicy.pdfPageToRender(request.pageNumber)
        val pageWasCited = FindThumbnailPolicy.pageWasCited(request.pageNumber)
        val record = assetRepository.find(
            AssetIdentity(SourceId(request.sourceId), SourceAssetKey(request.sourceAssetKey)),
        ) ?: return FindThumbnailResult.Glyph(FindThumbnailGlyph.SOURCE_UNREACHABLE)
        val asset = record.asset
        if (asset.type != AssetType.PDF) {
            return FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE)
        }
        val signal = CancellationSignal()
        val cancelHandle = coroutineContext[Job]?.invokeOnCompletion { signal.cancel() }
        try {
            val outcome = descriptorAccess.withReadOnlyDescriptor(
                request = PdfExtractionRequest(asset = asset, schemaVersion = EXTRACTION_SCHEMA),
                cancellationSignal = signal,
            ) { pfd ->
                pagePreviewRenderer.renderPage(
                    descriptor = pfd,
                    pageNumber = pageNumber,
                    documentLabel = request.label,
                    maxEdgePx = FindThumbnailLimits.MAX_EDGE_PX,
                )
            }
            return thumbnailFromPdfOutcome(outcome, pageWasCited)
        } finally {
            cancelHandle?.dispose()
        }
    }

    private fun thumbnailFromPdfOutcome(
        outcome: PdfReadOnlyDescriptorOutcome<PdfPagePreviewRenderResult>,
        pageWasCited: Boolean,
    ): FindThumbnailResult = when (outcome) {
        is PdfReadOnlyDescriptorOutcome.Consumed -> when (val render = outcome.value) {
            is PdfPagePreviewRenderResult.Ready -> FindThumbnailResult.Ready(
                widthPx = render.widthPx,
                heightPx = render.heightPx,
                argb8888 = render.argb8888,
                renderedPageNumber = render.pageNumber,
                pageWasCited = pageWasCited,
            )
            PdfPagePreviewRenderResult.SourceUnavailable ->
                FindThumbnailResult.Glyph(FindThumbnailGlyph.SOURCE_UNREACHABLE)
            PdfPagePreviewRenderResult.CouldNotOpen ->
                FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE)
        }
        PdfReadOnlyDescriptorOutcome.AccessRevoked,
        PdfReadOnlyDescriptorOutcome.SourceUnavailable,
        PdfReadOnlyDescriptorOutcome.SourceMismatch,
        PdfReadOnlyDescriptorOutcome.StaleSource,
        PdfReadOnlyDescriptorOutcome.InvalidTarget,
        PdfReadOnlyDescriptorOutcome.TreeMembershipDenied,
        -> FindThumbnailResult.Glyph(FindThumbnailGlyph.SOURCE_UNREACHABLE)
        PdfReadOnlyDescriptorOutcome.AccessRequired,
        PdfReadOnlyDescriptorOutcome.UnsupportedPlatform,
        PdfReadOnlyDescriptorOutcome.Cancelled,
        PdfReadOnlyDescriptorOutcome.RetryableFailure,
        -> FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE)
    }

    private companion object {
        val EXTRACTION_SCHEMA = ExtractionSchemaVersion("pdf-extraction-v1")
    }
}
