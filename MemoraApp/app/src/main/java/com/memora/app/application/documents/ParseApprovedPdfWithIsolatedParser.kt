package com.memora.app.application.documents

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.memora.app.domain.extraction.PdfExtractionRequest

/**
 * Parses one broker-handed read-only PDF descriptor through the isolated parser service.
 */
internal fun interface ApprovedPdfBorrowedParser {
    fun parseBorrowed(
        descriptor: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
    ): ApprovedPdfParsingOutcome
}

/**
 * Connects one approved descriptor to the isolated parser without making it searchable.
 *
 * Returns only content-free parser status. It does not persist an extraction, create
 * a Memory, invoke understanding/AI, or expose source locations above this boundary.
 */
internal class ParseApprovedPdfWithIsolatedParser(
    private val descriptorAccess: PdfReadOnlyDescriptorAccess,
    private val parser: ApprovedPdfBorrowedParser,
) : ApprovedPdfParsingPort {
    override suspend fun execute(
        request: PdfExtractionRequest,
        cancellationSignal: CancellationSignal?,
    ): ApprovedPdfParsingOutcome = when (
        val brokerResult = descriptorAccess.withReadOnlyDescriptor(
            request = request,
            cancellationSignal = cancellationSignal,
        ) { descriptor ->
            parser.parseBorrowed(descriptor, cancellationSignal)
        }
    ) {
        is PdfReadOnlyDescriptorOutcome.Consumed -> brokerResult.value
        PdfReadOnlyDescriptorOutcome.AccessRequired -> ApprovedPdfParsingOutcome.AccessRequired
        PdfReadOnlyDescriptorOutcome.AccessRevoked -> ApprovedPdfParsingOutcome.AccessRevoked
        PdfReadOnlyDescriptorOutcome.SourceUnavailable -> ApprovedPdfParsingOutcome.SourceUnavailable
        PdfReadOnlyDescriptorOutcome.SourceMismatch -> ApprovedPdfParsingOutcome.SourceMismatch
        PdfReadOnlyDescriptorOutcome.StaleSource -> ApprovedPdfParsingOutcome.StaleSource
        PdfReadOnlyDescriptorOutcome.InvalidTarget,
        PdfReadOnlyDescriptorOutcome.TreeMembershipDenied,
        PdfReadOnlyDescriptorOutcome.UnsupportedPlatform,
        -> ApprovedPdfParsingOutcome.SourceUnavailable
        PdfReadOnlyDescriptorOutcome.Cancelled -> ApprovedPdfParsingOutcome.Cancelled
        PdfReadOnlyDescriptorOutcome.RetryableFailure -> ApprovedPdfParsingOutcome.RetryableFailure
    }

    suspend fun execute(request: PdfExtractionRequest): ApprovedPdfParsingOutcome =
        execute(request, cancellationSignal = null)
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

    data object StaleSource : ApprovedPdfParsingOutcome

    data object Cancelled : ApprovedPdfParsingOutcome

    data object ParserFailure : ApprovedPdfParsingOutcome

    data object RetryableFailure : ApprovedPdfParsingOutcome
}
