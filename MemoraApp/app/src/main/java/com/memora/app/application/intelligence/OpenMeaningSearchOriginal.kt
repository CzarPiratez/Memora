package com.memora.app.application.intelligence

import com.memora.app.application.documents.OpenPersistedPdfForViewing
import com.memora.app.application.documents.PdfPagePreviewRenderResult
import com.memora.app.application.images.OpenPersistedPhotoForViewing
import com.memora.app.application.images.OpenPersistedScreenshotForViewing
import com.memora.app.application.images.PhotoPreviewRenderResult
import com.memora.app.application.images.ScreenshotPreviewRenderResult
import com.memora.app.application.notes.OpenPersistedNotePageInOneNote
import com.memora.app.application.notes.OpenPersistedNotePageResult
import com.memora.app.domain.asset.AssetType
import javax.inject.Inject

/**
 * Opens the original Asset for a meaning-search hit (E5b2b / E5b2d).
 *
 * For PDFs, opens the page cited by the Memory summary evidence (`pdf:page:N`)
 * when known; otherwise falls back to page 1 without inventing a query-best page.
 */
class OpenMeaningSearchOriginal @Inject constructor(
    private val openScreenshot: OpenPersistedScreenshotForViewing,
    private val openPhoto: OpenPersistedPhotoForViewing,
    private val openPdf: OpenPersistedPdfForViewing,
    private val openNote: OpenPersistedNotePageInOneNote,
) {
    suspend operator fun invoke(hit: MeaningSearchHit): MeaningOpenOriginalResult =
        when (hit.assetType) {
            AssetType.SCREENSHOT -> when (
                val outcome = openScreenshot(
                    sourceId = hit.sourceId.value,
                    sourceAssetKey = hit.sourceAssetKey.value,
                    screenshotLabel = hit.label,
                )
            ) {
                is ScreenshotPreviewRenderResult.Ready -> MeaningOpenOriginalResult.ScreenshotReady(
                    label = outcome.screenshotLabel,
                    widthPx = outcome.widthPx,
                    heightPx = outcome.heightPx,
                    argb8888 = outcome.argb8888,
                )
                ScreenshotPreviewRenderResult.SourceUnavailable ->
                    MeaningOpenOriginalResult.SourceUnavailable
                ScreenshotPreviewRenderResult.CouldNotOpen ->
                    MeaningOpenOriginalResult.CouldNotOpen
            }

            AssetType.PHOTO -> when (
                val outcome = openPhoto(
                    sourceId = hit.sourceId.value,
                    sourceAssetKey = hit.sourceAssetKey.value,
                    photoLabel = hit.label,
                )
            ) {
                is PhotoPreviewRenderResult.Ready -> MeaningOpenOriginalResult.PhotoReady(
                    label = outcome.photoLabel,
                    widthPx = outcome.widthPx,
                    heightPx = outcome.heightPx,
                    argb8888 = outcome.argb8888,
                )
                PhotoPreviewRenderResult.SourceUnavailable ->
                    MeaningOpenOriginalResult.SourceUnavailable
                PhotoPreviewRenderResult.CouldNotOpen ->
                    MeaningOpenOriginalResult.CouldNotOpen
            }

            AssetType.PDF -> when (
                val outcome = openPdf(
                    sourceId = hit.sourceId.value,
                    sourceAssetKey = hit.sourceAssetKey.value,
                    pageNumber = resolvePdfPage(hit),
                    documentLabel = hit.label,
                )
            ) {
                is PdfPagePreviewRenderResult.Ready -> MeaningOpenOriginalResult.PdfReady(
                    label = outcome.documentLabel,
                    pageNumber = outcome.pageNumber,
                    pageCount = outcome.pageCount,
                    widthPx = outcome.widthPx,
                    heightPx = outcome.heightPx,
                    argb8888 = outcome.argb8888,
                    openedCitedPage = hit.citedPdfPageNumber != null,
                )
                PdfPagePreviewRenderResult.SourceUnavailable ->
                    MeaningOpenOriginalResult.SourceUnavailable
                PdfPagePreviewRenderResult.CouldNotOpen ->
                    MeaningOpenOriginalResult.CouldNotOpen
            }

            AssetType.NOTE -> when (
                val outcome = openNote(
                    sourceId = hit.sourceId.value,
                    sourceAssetKey = hit.sourceAssetKey.value,
                )
            ) {
                is OpenPersistedNotePageResult.Ready -> MeaningOpenOriginalResult.NoteReady(
                    webUrl = outcome.webUrl,
                    clientUrl = outcome.clientUrl,
                )
                OpenPersistedNotePageResult.SourceUnavailable ->
                    MeaningOpenOriginalResult.SourceUnavailable
                OpenPersistedNotePageResult.CouldNotOpen ->
                    MeaningOpenOriginalResult.CouldNotOpen
            }
        }

    companion object {
        const val PDF_FALLBACK_PAGE = 1

        fun resolvePdfPage(hit: MeaningSearchHit): Int =
            hit.citedPdfPageNumber?.takeIf { it > 0 } ?: PDF_FALLBACK_PAGE
    }
}

sealed interface MeaningOpenOriginalResult {
    data class ScreenshotReady(
        val label: String,
        val widthPx: Int,
        val heightPx: Int,
        val argb8888: IntArray,
    ) : MeaningOpenOriginalResult

    data class PhotoReady(
        val label: String,
        val widthPx: Int,
        val heightPx: Int,
        val argb8888: IntArray,
    ) : MeaningOpenOriginalResult

    data class PdfReady(
        val label: String,
        val pageNumber: Int,
        val pageCount: Int,
        val widthPx: Int,
        val heightPx: Int,
        val argb8888: IntArray,
        /** True when open used the Memory summary's cited page, not the fallback. */
        val openedCitedPage: Boolean = false,
    ) : MeaningOpenOriginalResult

    data class NoteReady(
        val webUrl: String?,
        val clientUrl: String?,
    ) : MeaningOpenOriginalResult

    data object SourceUnavailable : MeaningOpenOriginalResult

    data object CouldNotOpen : MeaningOpenOriginalResult
}
