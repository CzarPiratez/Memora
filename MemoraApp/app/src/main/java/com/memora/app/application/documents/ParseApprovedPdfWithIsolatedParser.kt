package com.memora.app.application.documents

import android.os.CancellationSignal
import com.memora.app.data.pdfbox.isolation.BorrowedPdfDescriptorParser
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClientOutcome
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClientResult
import com.memora.app.data.saf.SafPdfDescriptorBroker
import com.memora.app.data.saf.SafPdfDescriptorBrokerResult
import com.memora.app.domain.extraction.PdfExtractionRequest

/**
 * Connects one approved descriptor to the isolated parser without making it searchable.
 *
 * This unbound application coordinator is intentionally not injected, scheduled, or called by
 * the UI. It returns only content-free parser status. It does not persist an extraction, create
 * a Memory, invoke understanding/AI, or expose source locations above this boundary.
 */
internal class ParseApprovedPdfWithIsolatedParser(
    private val descriptorBroker: SafPdfDescriptorBroker,
    private val parser: BorrowedPdfDescriptorParser,
) : ApprovedPdfParsingPort {
    override suspend fun execute(
        request: PdfExtractionRequest,
        cancellationSignal: CancellationSignal?,
    ): ApprovedPdfParsingOutcome = when (
        val brokerResult = descriptorBroker.withReadOnlyDescriptor(request, cancellationSignal) {
            parser.parseBorrowed(it, cancellationSignal)
        }
    ) {
        is SafPdfDescriptorBrokerResult.Consumed -> brokerResult.value.toOutcome()
        SafPdfDescriptorBrokerResult.AccessRequired -> ApprovedPdfParsingOutcome.AccessRequired
        SafPdfDescriptorBrokerResult.AccessRevoked -> ApprovedPdfParsingOutcome.AccessRevoked
        SafPdfDescriptorBrokerResult.SourceUnavailable -> ApprovedPdfParsingOutcome.SourceUnavailable
        SafPdfDescriptorBrokerResult.SourceMismatch -> ApprovedPdfParsingOutcome.SourceMismatch
        SafPdfDescriptorBrokerResult.InvalidTarget,
        SafPdfDescriptorBrokerResult.TreeMembershipDenied,
        SafPdfDescriptorBrokerResult.UnsupportedPlatform,
        -> ApprovedPdfParsingOutcome.SourceUnavailable
        SafPdfDescriptorBrokerResult.Cancelled -> ApprovedPdfParsingOutcome.Cancelled
        SafPdfDescriptorBrokerResult.RetryableFailure -> ApprovedPdfParsingOutcome.RetryableFailure
    }

    suspend fun execute(request: PdfExtractionRequest): ApprovedPdfParsingOutcome =
        execute(request, cancellationSignal = null)

    private fun IsolatedPdfParserClientResult.toOutcome(): ApprovedPdfParsingOutcome = when (outcome) {
        IsolatedPdfParserClientOutcome.EXTRACTED -> pageOutcome { pageCount ->
            ApprovedPdfParsingOutcome.Extracted(pageCount)
        }
        IsolatedPdfParserClientOutcome.NO_EXTRACTABLE_TEXT -> pageOutcome { pageCount ->
            ApprovedPdfParsingOutcome.NoExtractableText(pageCount)
        }
        IsolatedPdfParserClientOutcome.PASSWORD_PROTECTED -> ApprovedPdfParsingOutcome.PasswordProtected
        IsolatedPdfParserClientOutcome.FAILURE -> if (retryable) {
            ApprovedPdfParsingOutcome.RetryableFailure
        } else {
            ApprovedPdfParsingOutcome.ParserFailure
        }
    }

    private inline fun IsolatedPdfParserClientResult.pageOutcome(
        create: (Int) -> ApprovedPdfParsingOutcome,
    ): ApprovedPdfParsingOutcome = if (!retryable && pageCount != null && pageCount > 0) {
        create(pageCount)
    } else {
        ApprovedPdfParsingOutcome.RetryableFailure
    }
}

/**
 * Application port for one approved PDF handoff.
 *
 * Its production implementation retains the established broker and isolated-parser
 * boundaries. It deliberately returns a content-free status; it cannot expose a source
 * handle, URI, or page text.
 */
internal fun interface ApprovedPdfParsingPort {
    suspend fun execute(
        request: PdfExtractionRequest,
        cancellationSignal: CancellationSignal?,
    ): ApprovedPdfParsingOutcome
}

/** Content-free outcome for a future persistence boundary; no result here is searchable yet. */
internal sealed interface ApprovedPdfParsingOutcome {
    data class Extracted(val pageCount: Int) : ApprovedPdfParsingOutcome

    data class NoExtractableText(val pageCount: Int) : ApprovedPdfParsingOutcome

    data object PasswordProtected : ApprovedPdfParsingOutcome

    data object AccessRequired : ApprovedPdfParsingOutcome

    data object AccessRevoked : ApprovedPdfParsingOutcome

    data object SourceUnavailable : ApprovedPdfParsingOutcome

    data object SourceMismatch : ApprovedPdfParsingOutcome

    data object Cancelled : ApprovedPdfParsingOutcome

    data object ParserFailure : ApprovedPdfParsingOutcome

    data object RetryableFailure : ApprovedPdfParsingOutcome
}
