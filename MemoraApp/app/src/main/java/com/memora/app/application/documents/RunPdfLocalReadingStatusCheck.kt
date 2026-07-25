package com.memora.app.application.documents

import android.content.Context
import android.os.CancellationSignal
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.pdfbox.isolation.AndroidIsolatedPdfParserConnection
import com.memora.app.data.pdfbox.isolation.ContextIsolatedPdfParserServiceBinder
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserBindingStatus
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClient
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserDescriptorHandoff
import com.memora.app.data.saf.ContentResolverSafPdfDescriptorPlatform
import com.memora.app.data.saf.SafPdfDescriptorBroker
import com.memora.app.data.saf.SafPdfDescriptorBrokerResult
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
 * Foreground local reading check that may persist searchable PDF extraction text.
 *
 * Opens one indexed PDF from the connected folder through the approved broker and private
 * isolated parser, maps a validated wire result, and atomically writes eligible complete /
 * no-text records to Room. It does not schedule WorkManager, invoke AI, or use the network.
 */
class RunPdfLocalReadingStatusCheck @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val findPdfFolderConnection: PdfFolderConnectionFinder,
    private val assetRepository: AssetRepository,
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val accessValidator: DocumentTreeAccessValidator,
    private val database: MemoraDatabase,
) {
    private val persistValidated = PersistValidatedPdfLocalReading(database)

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

        val request = PdfExtractionRequest(
            asset = asset,
            schemaVersion = EXTRACTION_SCHEMA,
        )

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

            val broker = SafPdfDescriptorBroker(
                approvalRepository = approvalRepository,
                accessValidator = accessValidator,
                platform = ContentResolverSafPdfDescriptorPlatform(context),
            )
            val handoff = IsolatedPdfParserDescriptorHandoff(client)
            when (
                val brokerResult = broker.withReadOnlyDescriptor(request, cancellationSignal) {
                    handoff.parseBorrowed(it, cancellationSignal)
                }
            ) {
                is SafPdfDescriptorBrokerResult.Consumed -> {
                    persistValidated.execute(request, brokerResult.value)
                }
                SafPdfDescriptorBrokerResult.AccessRequired,
                SafPdfDescriptorBrokerResult.AccessRevoked,
                -> PdfLocalReadingStatusCheckResult.AccessRecoveryNeeded
                SafPdfDescriptorBrokerResult.Cancelled -> PdfLocalReadingStatusCheckResult.Cancelled
                SafPdfDescriptorBrokerResult.SourceUnavailable,
                SafPdfDescriptorBrokerResult.SourceMismatch,
                SafPdfDescriptorBrokerResult.StaleSource,
                SafPdfDescriptorBrokerResult.InvalidTarget,
                SafPdfDescriptorBrokerResult.TreeMembershipDenied,
                SafPdfDescriptorBrokerResult.UnsupportedPlatform,
                SafPdfDescriptorBrokerResult.RetryableFailure,
                -> PdfLocalReadingStatusCheckResult.RetryableProblem
            }
        } finally {
            client.close()
            connection.close()
        }
    }

    private companion object {
        val EXTRACTION_SCHEMA = ExtractionSchemaVersion("pdf-extraction-v1")
        const val BIND_TIMEOUT_MILLIS = 10_000L
    }
}

/** Content-free result for the Local PDF reading UI; never carries page text. */
sealed interface PdfLocalReadingStatusCheckResult {
    /** Eligible extraction was persisted for search on this phone. */
    data object Completed : PdfLocalReadingStatusCheckResult

    data object PasswordProtected : PdfLocalReadingStatusCheckResult

    data object AccessRecoveryNeeded : PdfLocalReadingStatusCheckResult

    data object RetryableProblem : PdfLocalReadingStatusCheckResult

    data object Cancelled : PdfLocalReadingStatusCheckResult
}
