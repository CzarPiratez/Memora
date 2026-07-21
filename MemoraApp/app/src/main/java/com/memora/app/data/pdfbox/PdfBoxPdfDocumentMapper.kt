package com.memora.app.data.pdfbox

import com.memora.app.domain.extraction.PdfExtractionOutcome
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
import java.io.InputStream
import java.time.Clock

/**
 * Maps one already-authorized PDF byte stream to Memora's deterministic domain record.
 *
 * This class deliberately has no Android URI, ContentResolver, SAF, Room, Hilt, or UI
 * dependency. A later, separately reviewed platform adapter is responsible for checking
 * a persisted read grant before supplying an [InputStream]. This mapper always closes
 * the supplied stream and the parser document, retains no source bytes, and never
 * writes, uploads, or mutates the original source.
 */
class PdfBoxPdfDocumentMapper(
    private val clock: Clock = Clock.systemUTC(),
) {
    private val parser = PdfBoxPdfDocumentParser()

    fun extract(
        request: PdfExtractionRequest,
        input: InputStream,
    ): PdfExtractionOutcome = when (val parsed = parser.parse(input)) {
        is PdfDocumentParseResult.Parsed -> PdfExtractionOutcome.Extracted(
            PdfExtractionRecord.forRequest(
                request = request,
                title = parsed.title,
                pageCount = parsed.pageCount,
                metadata = parsed.metadata,
                pages = parsed.pages,
                textCoverage = parsed.textCoverage,
                extractedAt = clock.instant(),
            ),
        )

        PdfDocumentParseResult.PasswordProtected -> PdfExtractionOutcome.Failed(
            message = "This PDF is protected and cannot be read without its password.",
            retryable = false,
        )

        is PdfDocumentParseResult.Failed -> PdfExtractionOutcome.Failed(
            message = parsed.message,
            retryable = parsed.retryable,
        )
    }
}
