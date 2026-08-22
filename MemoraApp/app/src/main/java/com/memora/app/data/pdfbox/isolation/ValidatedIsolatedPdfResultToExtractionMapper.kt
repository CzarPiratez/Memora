package com.memora.app.data.pdfbox.isolation

import com.memora.app.domain.extraction.PdfExtractionOutcome
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
import com.memora.app.domain.extraction.PdfPageText
import com.memora.app.domain.extraction.PdfTextCoverage
import java.time.Clock

/**
 * Converts an already validated isolated-parser wire result into Memora's
 * deterministic PDF-extraction domain shape.
 *
 * The input type deliberately requires [IsolatedPdfParserWireResultValidation.Valid].
 * This mapper neither decodes Binder data nor opens a descriptor, URI, or source. It
 * keeps no lasting reference beyond the returned outcome and writes nothing itself.
 */
internal class ValidatedIsolatedPdfResultToExtractionMapper(
    private val clock: Clock = Clock.systemUTC(),
) {
    fun map(
        request: PdfExtractionRequest,
        validated: IsolatedPdfParserWireResultValidation.Valid,
    ): PdfExtractionOutcome = with(validated.result) {
        when (outcome) {
            IsolatedPdfParserWireOutcome.EXTRACTED -> extracted(
                request = request,
                pageCount = checkNotNull(pageCount),
                pages = chunks
                    .groupBy(IsolatedPdfParserPageTextChunk::pageNumber)
                    .toSortedMap()
                    .map { (pageNumber, pageChunks) ->
                        PdfPageText(
                            pageNumber = pageNumber,
                            text = pageChunks
                                .sortedBy(IsolatedPdfParserPageTextChunk::chunkIndex)
                                .joinToString(separator = "", transform = IsolatedPdfParserPageTextChunk::text),
                        )
                    },
                textCoverage = PdfTextCoverage.Complete,
            )

            IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT -> extracted(
                request = request,
                pageCount = checkNotNull(pageCount),
                pages = emptyList(),
                textCoverage = PdfTextCoverage.NoExtractableText,
            )

            IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED -> PdfExtractionOutcome.Failed(
                message = "This PDF is protected and cannot be read without its password.",
                retryable = false,
            )

            IsolatedPdfParserWireOutcome.FAILURE -> PdfExtractionOutcome.Failed(
                message = if (retryable) {
                    "UNFYND could not finish reading this PDF. You can try indexing it again later."
                } else {
                    "UNFYND could not safely process this PDF."
                },
                retryable = retryable,
            )
        }
    }

    private fun extracted(
        request: PdfExtractionRequest,
        pageCount: Int,
        pages: List<PdfPageText>,
        textCoverage: PdfTextCoverage,
    ): PdfExtractionOutcome.Extracted = PdfExtractionOutcome.Extracted(
        PdfExtractionRecord.forRequest(
            request = request,
            pageCount = pageCount,
            pages = pages,
            textCoverage = textCoverage,
            extractedAt = clock.instant(),
        ),
    )
}
