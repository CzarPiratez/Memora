package com.memora.app.application.documents

import android.os.CancellationSignal
import com.memora.app.domain.extraction.PdfExtractionOutcome
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
import com.memora.app.domain.extraction.PdfTextCoverage

/**
 * Combines an approved parser status with an already-created in-memory extraction result.
 *
 * This is an application-level consistency boundary only. The [extractionProvider] has no
 * descriptor, URI, source, Room, UI, worker, understanding, or network API; the current
 * synthetic implementation can therefore prove outcome handling without implying that page
 * text has been released from the isolated-parser client or persisted anywhere.
 */
internal class AssembleApprovedPdfExtraction(
    private val parser: ApprovedPdfParsingPort,
    private val extractionProvider: InMemoryPdfExtractionResultProvider,
) {
    suspend fun execute(
        request: PdfExtractionRequest,
        cancellationSignal: CancellationSignal? = null,
    ): ApprovedPdfExtractionAssemblyOutcome = when (
        val parserOutcome = parser.execute(request, cancellationSignal)
    ) {
        is ApprovedPdfParsingOutcome.Extracted -> combineSuccessfulOutcome(
            request = request,
            parserOutcome = parserOutcome,
            extraction = extractionProvider.extractionFor(request),
        )

        is ApprovedPdfParsingOutcome.NoExtractableText -> combineSuccessfulOutcome(
            request = request,
            parserOutcome = parserOutcome,
            extraction = extractionProvider.extractionFor(request),
        )

        ApprovedPdfParsingOutcome.PasswordProtected -> {
            ApprovedPdfExtractionAssemblyOutcome.PasswordProtected
        }
        ApprovedPdfParsingOutcome.AccessRequired -> ApprovedPdfExtractionAssemblyOutcome.AccessRequired
        ApprovedPdfParsingOutcome.AccessRevoked -> ApprovedPdfExtractionAssemblyOutcome.AccessRevoked
        ApprovedPdfParsingOutcome.SourceUnavailable -> ApprovedPdfExtractionAssemblyOutcome.SourceUnavailable
        ApprovedPdfParsingOutcome.SourceMismatch -> ApprovedPdfExtractionAssemblyOutcome.SourceMismatch
        ApprovedPdfParsingOutcome.StaleSource -> ApprovedPdfExtractionAssemblyOutcome.StaleSource
        ApprovedPdfParsingOutcome.Cancelled -> ApprovedPdfExtractionAssemblyOutcome.Cancelled
        ApprovedPdfParsingOutcome.ParserFailure -> ApprovedPdfExtractionAssemblyOutcome.ParserFailure
        ApprovedPdfParsingOutcome.RetryableFailure -> {
            ApprovedPdfExtractionAssemblyOutcome.RetryableParserFailure
        }
    }

    private fun combineSuccessfulOutcome(
        request: PdfExtractionRequest,
        parserOutcome: ApprovedPdfParsingOutcome,
        extraction: PdfExtractionOutcome,
    ): ApprovedPdfExtractionAssemblyOutcome = when (extraction) {
        is PdfExtractionOutcome.Extracted -> if (extraction.record.matches(request, parserOutcome)) {
            ApprovedPdfExtractionAssemblyOutcome.Extracted(extraction.record)
        } else {
            ApprovedPdfExtractionAssemblyOutcome.InconsistentExtraction
        }

        PdfExtractionOutcome.AccessRequired -> ApprovedPdfExtractionAssemblyOutcome.AccessRequired
        PdfExtractionOutcome.AccessRevoked -> ApprovedPdfExtractionAssemblyOutcome.AccessRevoked
        is PdfExtractionOutcome.Failed -> ApprovedPdfExtractionAssemblyOutcome.ExtractionFailure(
            retryable = extraction.retryable,
        )
    }

    private fun PdfExtractionRecord.matches(
        request: PdfExtractionRequest,
        parserOutcome: ApprovedPdfParsingOutcome,
    ): Boolean = assetIdentity == request.asset.identity &&
        assetFingerprint == request.asset.fingerprint &&
        schemaVersion == request.schemaVersion &&
        when (parserOutcome) {
            is ApprovedPdfParsingOutcome.Extracted -> {
                pageCount == parserOutcome.pageCount && textCoverage == PdfTextCoverage.Complete
            }

            is ApprovedPdfParsingOutcome.NoExtractableText -> {
                pageCount == parserOutcome.pageCount && textCoverage == PdfTextCoverage.NoExtractableText
            }

            else -> false
        }
}

/**
 * Supplies an already-created, in-memory extraction result for a request.
 *
 * The current use is synthetic-only. A future implementation may be introduced only after the
 * separate transport, streaming, source-custody, and persistence gates are accepted.
 */
internal fun interface InMemoryPdfExtractionResultProvider {
    fun extractionFor(request: PdfExtractionRequest): PdfExtractionOutcome
}

/** No member carries a URI, descriptor, source content, or persistence side effect. */
internal sealed interface ApprovedPdfExtractionAssemblyOutcome {
    data class Extracted(val record: PdfExtractionRecord) : ApprovedPdfExtractionAssemblyOutcome

    data object PasswordProtected : ApprovedPdfExtractionAssemblyOutcome

    data object AccessRequired : ApprovedPdfExtractionAssemblyOutcome

    data object AccessRevoked : ApprovedPdfExtractionAssemblyOutcome

    data object SourceUnavailable : ApprovedPdfExtractionAssemblyOutcome

    data object SourceMismatch : ApprovedPdfExtractionAssemblyOutcome

    data object StaleSource : ApprovedPdfExtractionAssemblyOutcome

    data object Cancelled : ApprovedPdfExtractionAssemblyOutcome

    data object ParserFailure : ApprovedPdfExtractionAssemblyOutcome

    data object RetryableParserFailure : ApprovedPdfExtractionAssemblyOutcome

    data class ExtractionFailure(val retryable: Boolean) : ApprovedPdfExtractionAssemblyOutcome

    /** The status and record disagree; no record may be treated as an extraction. */
    data object InconsistentExtraction : ApprovedPdfExtractionAssemblyOutcome
}
