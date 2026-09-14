package com.memora.app.application.preview

import android.os.CancellationSignal
import com.memora.app.application.documents.OpenPersistedPdfForViewing
import com.memora.app.application.documents.PdfPagePreviewRenderResult
import com.memora.app.application.images.OpenPersistedPhotoForViewing
import com.memora.app.application.images.OpenPersistedScreenshotForViewing
import com.memora.app.application.images.PhotoPreviewRenderResult
import com.memora.app.application.images.ScreenshotPreviewRenderResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.job

/**
 * Re-opens an already-viewed original at a larger decode budget for pinch-zoom.
 *
 * Search is unchanged. A failed reload is Unavailable so the last sharp-enough
 * preview stays on screen.
 */
@Singleton
class ReloadOriginalPreview(
    private val openPdf: suspend (
        sourceId: String,
        sourceAssetKey: String,
        pageNumber: Int,
        documentLabel: String,
        maxEdgePx: Int,
        cancellationSignal: CancellationSignal?,
    ) -> PdfPagePreviewRenderResult,
    private val openPhoto: suspend (
        sourceId: String,
        sourceAssetKey: String,
        photoLabel: String,
        maxEdgePx: Int,
    ) -> PhotoPreviewRenderResult,
    private val openScreenshot: suspend (
        sourceId: String,
        sourceAssetKey: String,
        screenshotLabel: String,
        maxEdgePx: Int,
    ) -> ScreenshotPreviewRenderResult,
) {
    @Inject
    constructor(
        openPdf: OpenPersistedPdfForViewing,
        openPhoto: OpenPersistedPhotoForViewing,
        openScreenshot: OpenPersistedScreenshotForViewing,
    ) : this(
        openPdf = { sourceId, sourceAssetKey, pageNumber, documentLabel, maxEdgePx, signal ->
            openPdf(
                sourceId = sourceId,
                sourceAssetKey = sourceAssetKey,
                pageNumber = pageNumber,
                documentLabel = documentLabel,
                cancellationSignal = signal,
                maxEdgePx = maxEdgePx,
            )
        },
        openPhoto = { sourceId, sourceAssetKey, photoLabel, maxEdgePx ->
            openPhoto(
                sourceId = sourceId,
                sourceAssetKey = sourceAssetKey,
                photoLabel = photoLabel,
                maxEdgePx = maxEdgePx,
            )
        },
        openScreenshot = { sourceId, sourceAssetKey, screenshotLabel, maxEdgePx ->
            openScreenshot(
                sourceId = sourceId,
                sourceAssetKey = sourceAssetKey,
                screenshotLabel = screenshotLabel,
                maxEdgePx = maxEdgePx,
            )
        },
    )

    suspend operator fun invoke(
        request: OriginalPreviewReloadRequest,
        maxEdgePx: Int,
    ): OriginalPreviewReloadResult {
        require(maxEdgePx > 0) { "Reload needs a positive decode edge." }
        require(maxEdgePx <= PreviewZoomPolicy.MAX_RENDER_EDGE_PX) {
            "Reload decode edge cannot exceed ${PreviewZoomPolicy.MAX_RENDER_EDGE_PX} px."
        }
        val signal = CancellationSignal()
        coroutineContext.job.invokeOnCompletion { signal.cancel() }
        return try {
            when (request) {
                is OriginalPreviewReloadRequest.Pdf -> mapPdf(
                    openPdf(
                        request.sourceId,
                        request.sourceAssetKey,
                        request.pageNumber,
                        request.documentLabel,
                        maxEdgePx,
                        signal,
                    ),
                )
                is OriginalPreviewReloadRequest.Photo -> mapPhoto(
                    openPhoto(
                        request.sourceId,
                        request.sourceAssetKey,
                        request.photoLabel,
                        maxEdgePx,
                    ),
                )
                is OriginalPreviewReloadRequest.Screenshot -> mapScreenshot(
                    openScreenshot(
                        request.sourceId,
                        request.sourceAssetKey,
                        request.screenshotLabel,
                        maxEdgePx,
                    ),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            OriginalPreviewReloadResult.Unavailable
        } catch (_: OutOfMemoryError) {
            // A 2048 px ARGB decode is ~16 MB. A phone that cannot spare it must
            // keep the preview it already has, not lose the whole screen.
            OriginalPreviewReloadResult.Unavailable
        }
    }

    private fun mapPdf(outcome: PdfPagePreviewRenderResult): OriginalPreviewReloadResult =
        when (outcome) {
            is PdfPagePreviewRenderResult.Ready -> OriginalPreviewReloadResult.Ready(
                widthPx = outcome.widthPx,
                heightPx = outcome.heightPx,
                argb8888 = outcome.argb8888,
            )
            PdfPagePreviewRenderResult.SourceUnavailable,
            PdfPagePreviewRenderResult.CouldNotOpen,
            -> OriginalPreviewReloadResult.Unavailable
        }

    private fun mapPhoto(outcome: PhotoPreviewRenderResult): OriginalPreviewReloadResult =
        when (outcome) {
            is PhotoPreviewRenderResult.Ready -> OriginalPreviewReloadResult.Ready(
                widthPx = outcome.widthPx,
                heightPx = outcome.heightPx,
                argb8888 = outcome.argb8888,
            )
            PhotoPreviewRenderResult.SourceUnavailable,
            PhotoPreviewRenderResult.CouldNotOpen,
            -> OriginalPreviewReloadResult.Unavailable
        }

    private fun mapScreenshot(outcome: ScreenshotPreviewRenderResult): OriginalPreviewReloadResult =
        when (outcome) {
            is ScreenshotPreviewRenderResult.Ready -> OriginalPreviewReloadResult.Ready(
                widthPx = outcome.widthPx,
                heightPx = outcome.heightPx,
                argb8888 = outcome.argb8888,
            )
            ScreenshotPreviewRenderResult.SourceUnavailable,
            ScreenshotPreviewRenderResult.CouldNotOpen,
            -> OriginalPreviewReloadResult.Unavailable
        }
}
