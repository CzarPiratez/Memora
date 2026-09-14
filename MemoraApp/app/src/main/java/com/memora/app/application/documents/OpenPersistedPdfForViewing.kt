package com.memora.app.application.documents

import android.os.CancellationSignal
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a read-only in-app preview of the cited PDF page for a keyword hit.
 *
 * Reopens the original only after the user taps Open. Search itself stays on
 * stored Room text. Does not edit, copy, or claim ownership of the file.
 */
@Singleton
class OpenPersistedPdfForViewing @Inject constructor(
    private val assetRepository: AssetRepository,
    private val descriptorAccess: PdfReadOnlyDescriptorAccess,
    private val pagePreviewRenderer: PdfPagePreviewRenderer,
) {
    suspend operator fun invoke(
        sourceId: String,
        sourceAssetKey: String,
        pageNumber: Int,
        documentLabel: String,
        cancellationSignal: CancellationSignal? = null,
        maxEdgePx: Int = PdfPreviewScale.OPEN_MAX_EDGE_PX,
    ): PdfPagePreviewRenderResult = withContext(Dispatchers.IO) {
        require(pageNumber > 0) { "Open original needs a positive page number." }
        require(sourceId.isNotBlank()) { "Open original needs a source id." }
        require(sourceAssetKey.isNotBlank()) { "Open original needs a source asset key." }
        require(documentLabel.isNotBlank()) { "Open original needs a document label." }
        require(maxEdgePx > 0) { "Open original needs a positive decode edge." }

        val record = assetRepository.find(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        ) ?: return@withContext PdfPagePreviewRenderResult.SourceUnavailable

        val asset = record.asset
        if (asset.type != AssetType.PDF) {
            return@withContext PdfPagePreviewRenderResult.CouldNotOpen
        }

        val request = PdfExtractionRequest(
            asset = asset,
            schemaVersion = EXTRACTION_SCHEMA,
        )

        when (
            val brokerResult = descriptorAccess.withReadOnlyDescriptor(
                request = request,
                cancellationSignal = cancellationSignal,
            ) { pfd ->
                pagePreviewRenderer.renderPage(
                    descriptor = pfd,
                    pageNumber = pageNumber,
                    documentLabel = documentLabel,
                    maxEdgePx = maxEdgePx,
                )
            }
        ) {
            is PdfReadOnlyDescriptorOutcome.Consumed -> brokerResult.value
            PdfReadOnlyDescriptorOutcome.AccessRequired,
            PdfReadOnlyDescriptorOutcome.AccessRevoked,
            PdfReadOnlyDescriptorOutcome.SourceUnavailable,
            PdfReadOnlyDescriptorOutcome.SourceMismatch,
            PdfReadOnlyDescriptorOutcome.StaleSource,
            PdfReadOnlyDescriptorOutcome.InvalidTarget,
            PdfReadOnlyDescriptorOutcome.TreeMembershipDenied,
            PdfReadOnlyDescriptorOutcome.UnsupportedPlatform,
            PdfReadOnlyDescriptorOutcome.Cancelled,
            -> PdfPagePreviewRenderResult.SourceUnavailable
            PdfReadOnlyDescriptorOutcome.RetryableFailure ->
                PdfPagePreviewRenderResult.CouldNotOpen
        }
    }

    private companion object {
        val EXTRACTION_SCHEMA = ExtractionSchemaVersion("pdf-extraction-v1")
    }
}
