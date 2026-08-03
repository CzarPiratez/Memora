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
import com.memora.app.domain.extraction.SavedPdfPageTextSource
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingSimilarity
import javax.inject.Inject

/**
 * Opens the original Asset for a meaning-search hit (E5b2b / E5b2d / E5b2e).
 *
 * For PDFs: prefers a cue-best saved page when the on-device embedder clearly
 * ranks it above the Memory summary cite; otherwise opens the cited page
 * (`pdf:page:N`) or page 1.
 */
class OpenMeaningSearchOriginal @Inject constructor(
    private val openScreenshot: OpenPersistedScreenshotForViewing,
    private val openPhoto: OpenPersistedPhotoForViewing,
    private val openPdf: OpenPersistedPdfForViewing,
    private val openNote: OpenPersistedNotePageInOneNote,
    private val embeddingEngine: EmbeddingEngine,
    private val savedPdfPages: SavedPdfPageTextSource,
) {
    suspend operator fun invoke(
        hit: MeaningSearchHit,
        query: String = "",
    ): MeaningOpenOriginalResult =
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

            AssetType.PDF -> {
                val decision = resolvePdfOpenPage(hit, query)
                when (
                    val outcome = openPdf(
                        sourceId = hit.sourceId.value,
                        sourceAssetKey = hit.sourceAssetKey.value,
                        pageNumber = decision.pageNumber,
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
                        pageBasis = decision.basis,
                    )
                    PdfPagePreviewRenderResult.SourceUnavailable ->
                        MeaningOpenOriginalResult.SourceUnavailable
                    PdfPagePreviewRenderResult.CouldNotOpen ->
                        MeaningOpenOriginalResult.CouldNotOpen
                }
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

    suspend fun resolvePdfOpenPage(
        hit: MeaningSearchHit,
        query: String,
    ): MeaningPdfOpenPageDecision {
        val cited = hit.citedPdfPageNumber
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return ResolveMeaningPdfOpenPage.decide(cited, emptyList())
        }
        if (embeddingEngine.availability() !is CapabilityAvailability.Available) {
            return ResolveMeaningPdfOpenPage.decide(cited, emptyList())
        }
        val queryVector = when (val encoded = embeddingEngine.embedText(trimmedQuery)) {
            is EmbeddingEncodeResult.Success -> encoded.vector
            else -> return ResolveMeaningPdfOpenPage.decide(cited, emptyList())
        }
        val pages = savedPdfPages.listCurrentVerifiedPages(
            sourceId = hit.sourceId.value,
            sourceAssetKey = hit.sourceAssetKey.value,
        ).take(ResolveMeaningPdfOpenPage.MAX_PAGES_TO_SCORE)
        if (pages.isEmpty()) {
            return ResolveMeaningPdfOpenPage.decide(cited, emptyList())
        }
        val scored = pages.mapNotNull { page ->
            val text = ResolveMeaningPdfOpenPage.truncateForEmbed(page.text)
            if (text.isEmpty()) return@mapNotNull null
            val pageVector = when (val encoded = embeddingEngine.embedText(text)) {
                is EmbeddingEncodeResult.Success -> encoded.vector
                else -> return@mapNotNull null
            }
            if (pageVector.dimensions != queryVector.dimensions) return@mapNotNull null
            ScoredPdfPage(
                pageNumber = page.pageNumber,
                score = EmbeddingSimilarity.cosine(queryVector, pageVector),
            )
        }
        return ResolveMeaningPdfOpenPage.decide(cited, scored)
    }

    companion object {
        const val PDF_FALLBACK_PAGE = ResolveMeaningPdfOpenPage.FALLBACK_PAGE

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
        val pageBasis: MeaningPdfOpenPageBasis = MeaningPdfOpenPageBasis.FALLBACK,
    ) : MeaningOpenOriginalResult

    data class NoteReady(
        val webUrl: String?,
        val clientUrl: String?,
    ) : MeaningOpenOriginalResult

    data object SourceUnavailable : MeaningOpenOriginalResult

    data object CouldNotOpen : MeaningOpenOriginalResult
}
