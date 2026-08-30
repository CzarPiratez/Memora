package com.memora.app.application.documents

import android.os.CancellationSignal
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRequest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Foreground local reading check that may persist searchable PDF extraction text.
 *
 * Opens the next pending PDF from the connected folder through the approved broker and private
 * isolated parser, maps a validated wire result, and atomically writes eligible complete /
 * no-text records to Room. It does not schedule WorkManager, invoke AI, or use the network.
 */
class RunPdfLocalReadingStatusCheck @Inject constructor(
    private val findPdfFolderConnection: PdfFolderConnectionFinder,
    private val assetRepository: AssetRepository,
    private val isolatedSession: PdfIsolatedLocalReadingSession,
    private val persister: ValidatedPdfLocalReadingPersister,
) {
    suspend operator fun invoke(
        cancellationSignal: CancellationSignal,
    ): PdfLocalReadingStatusCheckResult = withContext(Dispatchers.IO) {
        if (cancellationSignal.isCanceled) {
            return@withContext PdfLocalReadingStatusCheckResult.Cancelled
        }

        val sourceId = findPdfFolderConnection()
            ?: return@withContext PdfLocalReadingStatusCheckResult.AccessRecoveryNeeded

        val asset = assetRepository.findNextPdfPendingLocalReading(
            sourceId = sourceId,
            schemaVersion = EXTRACTION_SCHEMA.value,
        ) ?: return@withContext PdfLocalReadingStatusCheckResult.RetryableProblem

        val request = PdfExtractionRequest(
            asset = asset,
            schemaVersion = EXTRACTION_SCHEMA,
        )

        when (val parse = isolatedSession.parseApprovedRequest(request, cancellationSignal)) {
            is PdfIsolatedLocalReadingParseOutcome.Parsed ->
                persister.persist(request, parse.persistenceHandle)
            PdfIsolatedLocalReadingParseOutcome.ServiceUnavailable,
            PdfIsolatedLocalReadingParseOutcome.RetryableFailure,
            -> PdfLocalReadingStatusCheckResult.RetryableProblem
            PdfIsolatedLocalReadingParseOutcome.AccessStopped ->
                PdfLocalReadingStatusCheckResult.AccessRecoveryNeeded
            PdfIsolatedLocalReadingParseOutcome.Cancelled ->
                PdfLocalReadingStatusCheckResult.Cancelled
        }
    }

    private companion object {
        val EXTRACTION_SCHEMA = ExtractionSchemaVersion("pdf-extraction-v1")
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
