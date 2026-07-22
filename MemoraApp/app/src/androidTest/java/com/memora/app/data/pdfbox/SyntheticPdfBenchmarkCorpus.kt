package com.memora.app.data.pdfbox

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream

/**
 * Repository-owned, deterministic PDFs used only by parser benchmark instrumentation tests.
 *
 * The corpus is generated entirely in memory. It is deliberately small and does not establish
 * production input, page, memory, timeout, battery, or thermal limits.
 */
internal object SyntheticPdfBenchmarkCorpus {
    fun progressivelyLargerTextFixtures(): List<SyntheticPdfBenchmarkFixture> = listOf(
        createTextFixture(
            id = "generated_text_small",
            pageCount = 1,
            textCodeUnitsPerPage = 1_024,
        ),
        createTextFixture(
            id = "generated_text_medium",
            pageCount = 4,
            textCodeUnitsPerPage = 2_048,
        ),
        createTextFixture(
            id = "generated_text_larger",
            pageCount = 8,
            textCodeUnitsPerPage = 4_096,
        ),
    )

    private fun createTextFixture(
        id: String,
        pageCount: Int,
        textCodeUnitsPerPage: Int,
    ): SyntheticPdfBenchmarkFixture {
        require(pageCount > 0)
        require(textCodeUnitsPerPage > "page-$pageCount:".length)

        val bytes = ByteArrayOutputStream().use { output ->
            PDDocument().use { document ->
                repeat(pageCount) { pageIndex ->
                    val pageNumber = pageIndex + 1
                    val page = PDPage()
                    document.addPage(page)
                    PDPageContentStream(document, page).use { content ->
                        content.beginText()
                        content.setFont(PDType1Font.HELVETICA, 10f)
                        content.newLineAtOffset(36f, 756f)
                        content.showText(pageText(pageNumber, textCodeUnitsPerPage))
                        content.endText()
                    }
                }
                document.save(output)
            }
            output.toByteArray()
        }

        return SyntheticPdfBenchmarkFixture(
            id = id,
            expectedPageCount = pageCount,
            minimumExpectedTextCodeUnits = pageCount.toLong() * textCodeUnitsPerPage,
            bytes = bytes,
        )
    }

    private fun pageText(pageNumber: Int, requestedCodeUnits: Int): String {
        val prefix = "page-$pageNumber:"
        return buildString(requestedCodeUnits) {
            append(prefix)
            repeat(requestedCodeUnits - prefix.length) { offset ->
                append(PAYLOAD_ALPHABET[(pageNumber + offset) % PAYLOAD_ALPHABET.length])
            }
        }
    }

    private const val PAYLOAD_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789 "
}

internal data class SyntheticPdfBenchmarkFixture(
    val id: String,
    val expectedPageCount: Int,
    val minimumExpectedTextCodeUnits: Long,
    val bytes: ByteArray,
)
