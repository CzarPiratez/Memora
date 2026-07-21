package com.memora.app.data.pdfbox

import com.memora.app.domain.extraction.PdfExtractionOutcome
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
import com.memora.app.domain.extraction.PdfPageText
import com.memora.app.domain.extraction.PdfTextCoverage
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.IOException
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
    fun extract(
        request: PdfExtractionRequest,
        input: InputStream,
    ): PdfExtractionOutcome = try {
        input.use { source ->
            PDDocument.load(source, MemoryUsageSetting.setupMainMemoryOnly()).use { document ->
                val pageCount = document.numberOfPages
                val pages = (1..pageCount).map { pageNumber ->
                    PdfPageText(
                        pageNumber = pageNumber,
                        text = textForPage(document, pageNumber),
                    )
                }
                val information = document.documentInformation
                val coverage = if (pages.any { it.text.isNotBlank() }) {
                    PdfTextCoverage.Complete
                } else {
                    PdfTextCoverage.NoExtractableText
                }

                PdfExtractionOutcome.Extracted(
                    PdfExtractionRecord.forRequest(
                        request = request,
                        title = information.title.meaningfulOrNull(),
                        pageCount = pageCount,
                        metadata = sourceMetadata(
                            author = information.author,
                            subject = information.subject,
                            keywords = information.keywords,
                            creator = information.creator,
                            producer = information.producer,
                        ),
                        pages = if (coverage == PdfTextCoverage.NoExtractableText) emptyList() else pages,
                        textCoverage = coverage,
                        extractedAt = clock.instant(),
                    ),
                )
            }
        }
    } catch (_: InvalidPasswordException) {
        PdfExtractionOutcome.Failed(
            message = "This PDF is protected and cannot be read without its password.",
            retryable = false,
        )
    } catch (_: IOException) {
        PdfExtractionOutcome.Failed(
            message = "Memora could not read this PDF. You can try indexing it again later.",
            retryable = true,
        )
    } catch (_: RuntimeException) {
        PdfExtractionOutcome.Failed(
            message = "Memora could not safely process this PDF.",
            retryable = false,
        )
    }

    private fun textForPage(
        document: PDDocument,
        pageNumber: Int,
    ): String = PDFTextStripper().apply {
        startPage = pageNumber
        endPage = pageNumber
    }.getText(document).trim()

    private fun sourceMetadata(
        author: String?,
        subject: String?,
        keywords: String?,
        creator: String?,
        producer: String?,
    ): Map<String, String> = buildMap {
        author.meaningfulOrNull()?.let { put("author", it) }
        subject.meaningfulOrNull()?.let { put("subject", it) }
        keywords.meaningfulOrNull()?.let { put("keywords", it) }
        creator.meaningfulOrNull()?.let { put("creator", it) }
        producer.meaningfulOrNull()?.let { put("producer", it) }
    }

    private fun String?.meaningfulOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
