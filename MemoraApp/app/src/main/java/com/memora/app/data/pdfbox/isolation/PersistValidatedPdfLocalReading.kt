package com.memora.app.data.pdfbox.isolation

import com.memora.app.application.documents.ApprovedPdfExtractionAssemblyOutcome
import com.memora.app.application.documents.PdfLocalReadingStatusCheckResult
import com.memora.app.application.documents.PersistApprovedPdfExtraction
import com.memora.app.application.documents.PersistApprovedPdfExtractionOutcome
import com.memora.app.application.documents.PrepareApprovedPdfExtractionPersistence
import com.memora.app.domain.extraction.PdfExtractionOutcome
import com.memora.app.domain.extraction.PdfExtractionPersistencePort
import com.memora.app.domain.extraction.PdfExtractionRequest

/**
 * Maps one validated ordinary-process parser result into an atomic Room extraction write.
 *
 * Returns only content-free UI status. Partial sessions never reach this type because the
 * client omits validated payload unless assembly completed.
 */
internal class PersistValidatedPdfLocalReading(
    private val persistencePort: PdfExtractionPersistencePort,
) {
    private val mapper = ValidatedIsolatedPdfResultToExtractionMapper()
    private val preparePersistence = PrepareApprovedPdfExtractionPersistence()
    private val persistExtraction = PersistApprovedPdfExtraction(persistencePort)

    suspend fun execute(
        request: PdfExtractionRequest,
        clientResult: IsolatedPdfParserClientResult,
    ): PdfLocalReadingStatusCheckResult = when (clientResult.outcome) {
        IsolatedPdfParserClientOutcome.PASSWORD_PROTECTED ->
            PdfLocalReadingStatusCheckResult.PasswordProtected

        IsolatedPdfParserClientOutcome.FAILURE ->
            PdfLocalReadingStatusCheckResult.RetryableProblem

        IsolatedPdfParserClientOutcome.EXTRACTED,
        IsolatedPdfParserClientOutcome.NO_EXTRACTABLE_TEXT,
        -> {
            val validated = clientResult.validatedResult
                ?: return PdfLocalReadingStatusCheckResult.RetryableProblem
            when (val extraction = mapper.map(request, validated)) {
                is PdfExtractionOutcome.Extracted -> {
                    val assembly = ApprovedPdfExtractionAssemblyOutcome.Extracted(extraction.record)
                    val decision = preparePersistence.prepare(request, assembly)
                    when (persistExtraction.execute(decision, extraction.record)) {
                        PersistApprovedPdfExtractionOutcome.Persisted ->
                            PdfLocalReadingStatusCheckResult.Completed
                        else -> PdfLocalReadingStatusCheckResult.RetryableProblem
                    }
                }

                is PdfExtractionOutcome.Failed ->
                    PdfLocalReadingStatusCheckResult.RetryableProblem

                PdfExtractionOutcome.AccessRequired,
                PdfExtractionOutcome.AccessRevoked,
                -> PdfLocalReadingStatusCheckResult.AccessRecoveryNeeded
            }
        }
    }
}
