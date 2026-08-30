package com.memora.app.data.pdfbox.isolation

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.memora.app.application.documents.ApprovedPdfBorrowedParser
import com.memora.app.application.documents.ApprovedPdfParsingOutcome
import com.memora.app.application.documents.PdfValidatedParsePersistenceHandle

internal class IsolatedPdfApprovedBorrowedParser(
    private val handoff: IsolatedPdfParserDescriptorHandoff,
) : ApprovedPdfBorrowedParser {
    override fun parseBorrowed(
        descriptor: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
    ): ApprovedPdfParsingOutcome = handoff.parseBorrowed(descriptor, cancellationSignal)
        .toApprovedPdfParsingOutcome()
}

internal class WirePdfValidatedParsePersistenceHandle(
    val result: IsolatedPdfParserClientResult,
) : PdfValidatedParsePersistenceHandle

internal fun IsolatedPdfParserClientResult.toApprovedPdfParsingOutcome(): ApprovedPdfParsingOutcome =
    when (outcome) {
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
