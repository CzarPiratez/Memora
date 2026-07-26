package com.memora.app.application.documents

import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.RoomPdfExtractionPersistencePort
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClientOutcome
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClientResult
import com.memora.app.data.pdfbox.isolation.ValidatedIsolatedPdfResultToExtractionMapper
import com.memora.app.domain.extraction.PdfExtractionOutcome
import com.memora.app.domain.extraction.PdfExtractionRequest

/**
 * Maps one validated ordinary-process parser result into an atomic Room extraction write.
 *
 * Returns only content-free UI status. Partial sessions never reach this type because the
 * client omits [IsolatedPdfParserClientResult.validatedResult] unless assembly completed.
 *
 * Resolves the live database on each write so clear/reopen cannot leave a closed Room instance.
 */
internal class PersistValidatedPdfLocalReading(
    private val database: () -> MemoraDatabase,
) {
    constructor(database: MemoraDatabase) : this(database = { database })

    private val mapper = ValidatedIsolatedPdfResultToExtractionMapper()
    private val preparePersistence = PrepareApprovedPdfExtractionPersistence()
    private val persistExtraction = PersistApprovedPdfExtraction(
        RoomPdfExtractionPersistencePort(database),
    )

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
