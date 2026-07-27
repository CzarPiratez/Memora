package com.memora.app.application.documents

import android.content.Context
import android.os.CancellationSignal
import com.memora.app.data.saf.ContentResolverSafPdfDescriptorPlatform
import com.memora.app.data.saf.SafPdfDescriptorBroker
import com.memora.app.data.saf.SafPdfDescriptorBrokerResult
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRequest
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @param:ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val accessValidator: DocumentTreeAccessValidator,
    private val pagePreviewRenderer: PdfPagePreviewRenderer,
) {
    suspend operator fun invoke(
        sourceId: String,
        sourceAssetKey: String,
        pageNumber: Int,
        documentLabel: String,
        cancellationSignal: CancellationSignal? = null,
    ): PdfPagePreviewRenderResult = withContext(Dispatchers.IO) {
        require(pageNumber > 0) { "Open original needs a positive page number." }
        require(sourceId.isNotBlank()) { "Open original needs a source id." }
        require(sourceAssetKey.isNotBlank()) { "Open original needs a source asset key." }
        require(documentLabel.isNotBlank()) { "Open original needs a document label." }

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
        val broker = SafPdfDescriptorBroker(
            approvalRepository = approvalRepository,
            accessValidator = accessValidator,
            platform = ContentResolverSafPdfDescriptorPlatform(context),
        )

        when (
            val brokerResult = broker.withReadOnlyDescriptor(request, cancellationSignal) { pfd ->
                pagePreviewRenderer.renderPage(
                    descriptor = pfd,
                    pageNumber = pageNumber,
                    documentLabel = documentLabel,
                )
            }
        ) {
            is SafPdfDescriptorBrokerResult.Consumed -> brokerResult.value
            SafPdfDescriptorBrokerResult.AccessRequired,
            SafPdfDescriptorBrokerResult.AccessRevoked,
            SafPdfDescriptorBrokerResult.SourceUnavailable,
            SafPdfDescriptorBrokerResult.SourceMismatch,
            SafPdfDescriptorBrokerResult.StaleSource,
            SafPdfDescriptorBrokerResult.InvalidTarget,
            SafPdfDescriptorBrokerResult.TreeMembershipDenied,
            SafPdfDescriptorBrokerResult.UnsupportedPlatform,
            SafPdfDescriptorBrokerResult.Cancelled,
            -> PdfPagePreviewRenderResult.SourceUnavailable
            SafPdfDescriptorBrokerResult.RetryableFailure ->
                PdfPagePreviewRenderResult.CouldNotOpen
        }
    }

    private companion object {
        val EXTRACTION_SCHEMA = ExtractionSchemaVersion("pdf-extraction-v1")
    }
}
