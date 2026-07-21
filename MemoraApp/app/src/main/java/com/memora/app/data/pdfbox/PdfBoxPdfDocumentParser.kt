package com.memora.app.data.pdfbox

import com.memora.app.domain.extraction.PdfPageText
import com.memora.app.domain.extraction.PdfTextCoverage
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.IOException
import java.io.InputStream

/**
 * Reads deterministic PDF facts without knowing where the document came from.
 *
 * This deliberately has no Asset, URI, ContentResolver, SAF, Room, Hilt, or UI
 * dependency. The ordinary process later binds these facts to the approved Asset
 * version; an isolated parser service can therefore receive only an already-opened
 * file descriptor rather than a source location or any source permission.
 */
internal class PdfBoxPdfDocumentParser {
    fun parse(input: InputStream): PdfDocumentParseResult = try {
        input.use { source ->
            PDDocument.load(source, MemoryUsageSetting.setupMainMemoryOnly()).use { document ->
                val pages = (1..document.numberOfPages).map { pageNumber ->
                    PdfPageText(
                        pageNumber = pageNumber,
                        text = textForPage(document, pageNumber),
                    )
                }
                val coverage = if (pages.any { it.text.isNotBlank() }) {
                    PdfTextCoverage.Complete
                } else {
                    PdfTextCoverage.NoExtractableText
                }
                val information = document.documentInformation

                PdfDocumentParseResult.Parsed(
                    title = information.title.meaningfulOrNull(),
                    pageCount = document.numberOfPages,
                    metadata = sourceMetadata(
                        author = information.author,
                        subject = information.subject,
                        keywords = information.keywords,
                        creator = information.creator,
                        producer = information.producer,
                    ),
                    pages = if (coverage == PdfTextCoverage.NoExtractableText) emptyList() else pages,
                    textCoverage = coverage,
                )
            }
        }
    } catch (_: InvalidPasswordException) {
        PdfDocumentParseResult.PasswordProtected
    } catch (_: IOException) {
        PdfDocumentParseResult.Failed(
            retryable = true,
            message = "Memora could not read this PDF. You can try indexing it again later.",
        )
    } catch (_: RuntimeException) {
        PdfDocumentParseResult.Failed(
            retryable = false,
            message = "Memora could not safely process this PDF.",
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

/** Raw parser facts, intentionally unbound to a Memora Asset or source location. */
internal sealed interface PdfDocumentParseResult {
    data class Parsed(
        val title: String?,
        val pageCount: Int,
        val metadata: Map<String, String>,
        val pages: List<PdfPageText>,
        val textCoverage: PdfTextCoverage,
    ) : PdfDocumentParseResult

    data object PasswordProtected : PdfDocumentParseResult

    data class Failed(
        val retryable: Boolean,
        val message: String,
    ) : PdfDocumentParseResult
}
