package com.memora.app.data.pdfbox.isolation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IsolatedPdfParserResultContractTest {
    @Test
    fun `accepts complete bounded chunks for every page`() {
        val result = extracted(
            pageCount = 2,
            chunks = listOf(
                chunk(pageNumber = 1, chunkIndex = 0, final = true, text = "First"),
                chunk(pageNumber = 2, chunkIndex = 0, final = false, text = "Sec"),
                chunk(pageNumber = 2, chunkIndex = 1, final = true, text = "ond"),
            ),
        )

        assertEquals(
            IsolatedPdfParserWireResultValidation.Valid(result),
            IsolatedPdfParserResultContract.validate(result, limits()),
        )
    }

    @Test
    fun `accepts a blank page as an explicit complete chunk`() {
        val result = extracted(
            pageCount = 1,
            chunks = listOf(chunk(pageNumber = 1, chunkIndex = 0, final = true, text = "")),
        )

        assertTrue(IsolatedPdfParserResultContract.validate(result, limits()) is IsolatedPdfParserWireResultValidation.Valid)
    }

    @Test
    fun `rejects an unknown schema version without exposing candidate text`() {
        val result = extracted(
            schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION + 1,
            pageCount = 1,
            chunks = listOf(chunk(pageNumber = 1, chunkIndex = 0, final = true, text = "secret")),
        )

        assertRejected(result)
    }

    @Test
    fun `rejects a page count beyond the supplied limit`() {
        val result = extracted(
            pageCount = 3,
            chunks = (1..3).map { pageNumber ->
                chunk(pageNumber = pageNumber, chunkIndex = 0, final = true, text = "x")
            },
        )

        assertRejected(result, limits(maximumPageCount = 2))
    }

    @Test
    fun `rejects page text larger than the supplied per-page limit`() {
        val result = extracted(
            pageCount = 1,
            chunks = listOf(
                chunk(pageNumber = 1, chunkIndex = 0, final = false, text = "abc"),
                chunk(pageNumber = 1, chunkIndex = 1, final = true, text = "def"),
            ),
        )

        assertRejected(result, limits(maximumPageTextCodeUnits = 5))
    }

    @Test
    fun `rejects total text larger than the supplied result limit`() {
        val result = extracted(
            pageCount = 2,
            chunks = listOf(
                chunk(pageNumber = 1, chunkIndex = 0, final = true, text = "abcd"),
                chunk(pageNumber = 2, chunkIndex = 0, final = true, text = "efgh"),
            ),
        )

        assertRejected(result, limits(maximumTotalTextCodeUnits = 7))
    }

    @Test
    fun `rejects incomplete chunk sequences rather than accepting partial text`() {
        val result = extracted(
            pageCount = 1,
            chunks = listOf(chunk(pageNumber = 1, chunkIndex = 0, final = false, text = "partial")),
        )

        assertRejected(result)
    }

    @Test
    fun `rejects extracted output that omits a page`() {
        val result = extracted(
            pageCount = 2,
            chunks = listOf(chunk(pageNumber = 1, chunkIndex = 0, final = true, text = "only one")),
        )

        assertRejected(result)
    }

    @Test
    fun `accepts no text only when it has no page chunks`() {
        val noText = IsolatedPdfParserWireResult(
            schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
            outcome = IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT,
            retryable = false,
            pageCount = 1,
        )

        assertTrue(IsolatedPdfParserResultContract.validate(noText, limits()) is IsolatedPdfParserWireResultValidation.Valid)
        assertRejected(noText.copy(chunks = listOf(chunk(1, 0, true, "invented"))))
    }

    @Test
    fun `rejects any content attached to a failure`() {
        val failure = IsolatedPdfParserWireResult(
            schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
            outcome = IsolatedPdfParserWireOutcome.FAILURE,
            retryable = true,
            pageCount = 1,
        )

        assertRejected(failure)
    }

    private fun extracted(
        schemaVersion: Int = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
        pageCount: Int,
        chunks: List<IsolatedPdfParserPageTextChunk>,
    ): IsolatedPdfParserWireResult = IsolatedPdfParserWireResult(
        schemaVersion = schemaVersion,
        outcome = IsolatedPdfParserWireOutcome.EXTRACTED,
        retryable = false,
        pageCount = pageCount,
        chunks = chunks,
    )

    private fun chunk(
        pageNumber: Int,
        chunkIndex: Int,
        final: Boolean,
        text: String,
    ): IsolatedPdfParserPageTextChunk = IsolatedPdfParserPageTextChunk(
        pageNumber = pageNumber,
        chunkIndex = chunkIndex,
        isFinalChunk = final,
        text = text,
    )

    private fun limits(
        maximumPageCount: Int = 2,
        maximumPageTextCodeUnits: Long = 10,
        maximumTotalTextCodeUnits: Long = 12,
    ): IsolatedPdfParserResultLimits = IsolatedPdfParserResultLimits(
        maximumPageCount = maximumPageCount,
        maximumChunksPerPage = 2,
        maximumPageTextCodeUnits = maximumPageTextCodeUnits,
        maximumTotalTextCodeUnits = maximumTotalTextCodeUnits,
    )

    private fun assertRejected(
        result: IsolatedPdfParserWireResult,
        limits: IsolatedPdfParserResultLimits = limits(),
    ) {
        assertEquals(IsolatedPdfParserWireResultValidation.Rejected, IsolatedPdfParserResultContract.validate(result, limits))
    }
}
