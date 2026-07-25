package com.memora.app.application.documents

import android.content.Context
import android.os.CancellationSignal
import com.memora.app.data.pdfbox.isolation.AndroidIsolatedPdfParserConnection
import com.memora.app.data.pdfbox.isolation.ContextIsolatedPdfParserServiceBinder
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserBindingStatus
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClient
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserDescriptorHandoff
import com.memora.app.data.saf.ContentResolverSafPdfDescriptorPlatform
import com.memora.app.data.saf.SafPdfDescriptorBroker
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Foreground, status-only local reading check for one indexed PDF in the connected folder.
 *
 * Binds the private isolated parser for one request, returns content-free status, and never
 * persists extraction text, schedules WorkManager, invokes AI, or uses the network.
 */
class RunPdfLocalReadingStatusCheck @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val findPdfFolderConnection: PdfFolderConnectionFinder,
    private val assetRepository: AssetRepository,
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val accessValidator: DocumentTreeAccessValidator,
) {
    suspend operator fun invoke(
        cancellationSignal: CancellationSignal,
    ): PdfLocalReadingStatusCheckResult = withContext(Dispatchers.IO) {
        if (cancellationSignal.isCanceled) {
            return@withContext PdfLocalReadingStatusCheckResult.Cancelled
        }

        val sourceId = findPdfFolderConnection()
            ?: return@withContext PdfLocalReadingStatusCheckResult.AccessRecoveryNeeded

        val asset = assetRepository.findFirstBySourceAndType(sourceId, AssetType.PDF)
            ?: return@withContext PdfLocalReadingStatusCheckResult.RetryableProblem

        val connection = AndroidIsolatedPdfParserConnection(
            ContextIsolatedPdfParserServiceBinder(context),
        )
        val client = IsolatedPdfParserClient(connection)
        try {
            if (connection.connect() == IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE) {
                return@withContext PdfLocalReadingStatusCheckResult.RetryableProblem
            }
            if (connection.awaitAvailability(BIND_TIMEOUT_MILLIS) !=
                IsolatedPdfParserBindingStatus.AVAILABLE
            ) {
                return@withContext PdfLocalReadingStatusCheckResult.RetryableProblem
            }
            if (cancellationSignal.isCanceled) {
                return@withContext PdfLocalReadingStatusCheckResult.Cancelled
            }

            val parser = ParseApprovedPdfWithIsolatedParser(
                descriptorBroker = SafPdfDescriptorBroker(
                    approvalRepository = approvalRepository,
                    accessValidator = accessValidator,
                    platform = ContentResolverSafPdfDescriptorPlatform(context),
                ),
                parser = IsolatedPdfParserDescriptorHandoff(client),
            )
            parser.execute(
                PdfExtractionRequest(
                    asset = asset,
                    schemaVersion = EXTRACTION_SCHEMA,
                ),
                cancellationSignal,
            ).toStatusCheckResult()
        } finally {
            client.close()
            connection.close()
        }
    }

    private fun ApprovedPdfParsingOutcome.toStatusCheckResult(): PdfLocalReadingStatusCheckResult =
        when (this) {
            is ApprovedPdfParsingOutcome.Extracted,
            is ApprovedPdfParsingOutcome.NoExtractableText,
            -> PdfLocalReadingStatusCheckResult.Completed

            ApprovedPdfParsingOutcome.PasswordProtected ->
                PdfLocalReadingStatusCheckResult.PasswordProtected

            ApprovedPdfParsingOutcome.AccessRequired,
            ApprovedPdfParsingOutcome.AccessRevoked,
            -> PdfLocalReadingStatusCheckResult.AccessRecoveryNeeded

            ApprovedPdfParsingOutcome.Cancelled -> PdfLocalReadingStatusCheckResult.Cancelled

            ApprovedPdfParsingOutcome.SourceUnavailable,
            ApprovedPdfParsingOutcome.SourceMismatch,
            ApprovedPdfParsingOutcome.StaleSource,
            ApprovedPdfParsingOutcome.ParserFailure,
            ApprovedPdfParsingOutcome.RetryableFailure,
            -> PdfLocalReadingStatusCheckResult.RetryableProblem
        }

    private companion object {
        val EXTRACTION_SCHEMA = ExtractionSchemaVersion("pdf-extraction-v1")
        const val BIND_TIMEOUT_MILLIS = 10_000L
    }
}

/** Content-free result for the Local PDF reading UI; never carries page text. */
sealed interface PdfLocalReadingStatusCheckResult {
    data object Completed : PdfLocalReadingStatusCheckResult

    data object PasswordProtected : PdfLocalReadingStatusCheckResult

    data object AccessRecoveryNeeded : PdfLocalReadingStatusCheckResult

    data object RetryableProblem : PdfLocalReadingStatusCheckResult

    data object Cancelled : PdfLocalReadingStatusCheckResult
}
