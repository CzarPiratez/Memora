package com.memora.app.data.pdfbox.isolation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IsolatedPdfParserSessionAssemblerTest {
    @Test
    fun `assembles ordered chunks into a validated extracted result`() {
        val assembler = assembler()

        assertEquals(
            IsolatedPdfParserSessionState.Collecting(pageCount = 2, receivedChunkCount = 0),
            assembler.accept(opened(pageCount = 2)),
        )
        assertEquals(
            IsolatedPdfParserSessionState.Collecting(pageCount = 2, receivedChunkCount = 1),
            assembler.accept(chunkEvent(pageNumber = 1, chunkIndex = 0, final = true, text = "First")),
        )
        assertEquals(
            IsolatedPdfParserSessionState.Collecting(pageCount = 2, receivedChunkCount = 2),
            assembler.accept(chunkEvent(pageNumber = 2, chunkIndex = 0, final = false, text = "Sec")),
        )
        assertEquals(
            IsolatedPdfParserSessionState.Collecting(pageCount = 2, receivedChunkCount = 3),
            assembler.accept(chunkEvent(pageNumber = 2, chunkIndex = 1, final = true, text = "ond")),
        )

        val completed = assembler.accept(IsolatedPdfParserSessionEvent.Completed)
        assertTrue(completed is IsolatedPdfParserSessionState.Completed)
        val result = (completed as IsolatedPdfParserSessionState.Completed).result
        assertEquals(IsolatedPdfParserWireOutcome.EXTRACTED, result.outcome)
        assertEquals(2, result.pageCount)
        assertEquals(3, result.chunks.size)
    }

    @Test
    fun `accepts a validated no-text header without retaining chunks`() {
        val assembler = assembler()
        val completed = assembler.accept(
            IsolatedPdfParserSessionEvent.Opened(
                IsolatedPdfParserSessionHeader(
                    schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                    outcome = IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT,
                    retryable = false,
                    pageCount = 1,
                ),
            ),
        )

        assertTrue(completed is IsolatedPdfParserSessionState.Completed)
        assertEquals(
            IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT,
            (completed as IsolatedPdfParserSessionState.Completed).result.outcome,
        )
        assertTrue(completed.result.chunks.isEmpty())
    }

    @Test
    fun `cancellation discards an in-flight session without exposing text`() {
        val assembler = assembler()
        assembler.accept(opened(pageCount = 1))
        assembler.accept(chunkEvent(pageNumber = 1, chunkIndex = 0, final = false, text = "secret"))

        assertEquals(
            IsolatedPdfParserSessionState.Cancelled,
            assembler.accept(IsolatedPdfParserSessionEvent.Cancelled),
        )
        assertEquals(
            IsolatedPdfParserSessionState.Cancelled,
            assembler.accept(IsolatedPdfParserSessionEvent.Completed),
        )
    }

    @Test
    fun `rejects out-of-order chunk indexes without completing`() {
        val assembler = assembler()
        assembler.accept(opened(pageCount = 1))

        assertEquals(
            IsolatedPdfParserSessionState.Rejected,
            assembler.accept(chunkEvent(pageNumber = 1, chunkIndex = 1, final = true, text = "skip")),
        )
    }

    @Test
    fun `rejects a chunk larger than the per-chunk ceiling`() {
        val assembler = assembler(maximumChunkTextCodeUnits = 3)
        assembler.accept(opened(pageCount = 1))

        assertEquals(
            IsolatedPdfParserSessionState.Rejected,
            assembler.accept(chunkEvent(pageNumber = 1, chunkIndex = 0, final = true, text = "abcd")),
        )
    }

    @Test
    fun `rejects completion when a page is still missing`() {
        val assembler = assembler()
        assembler.accept(opened(pageCount = 2))
        assembler.accept(chunkEvent(pageNumber = 1, chunkIndex = 0, final = true, text = "only"))

        assertEquals(
            IsolatedPdfParserSessionState.Rejected,
            assembler.accept(IsolatedPdfParserSessionEvent.Completed),
        )
    }

    @Test
    fun `rejects a chunk before the session is opened`() {
        val assembler = assembler()

        assertEquals(
            IsolatedPdfParserSessionState.Rejected,
            assembler.accept(chunkEvent(pageNumber = 1, chunkIndex = 0, final = true, text = "early")),
        )
    }

    @Test
    fun `rejects a second open after collecting has started`() {
        val assembler = assembler()
        assembler.accept(opened(pageCount = 1))

        assertEquals(
            IsolatedPdfParserSessionState.Rejected,
            assembler.accept(opened(pageCount = 1)),
        )
    }

    private fun assembler(
        maximumChunkTextCodeUnits: Long = 10,
    ): IsolatedPdfParserSessionAssembler = IsolatedPdfParserSessionAssembler(
        IsolatedPdfParserSessionLimits(
            resultLimits = IsolatedPdfParserResultLimits(
                maximumPageCount = 2,
                maximumChunksPerPage = 2,
                maximumPageTextCodeUnits = 10,
                maximumTotalTextCodeUnits = 12,
            ),
            maximumChunkTextCodeUnits = maximumChunkTextCodeUnits,
        ),
    )

    private fun opened(pageCount: Int): IsolatedPdfParserSessionEvent.Opened =
        IsolatedPdfParserSessionEvent.Opened(
            IsolatedPdfParserSessionHeader(
                schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                outcome = IsolatedPdfParserWireOutcome.EXTRACTED,
                retryable = false,
                pageCount = pageCount,
            ),
        )

    private fun chunkEvent(
        pageNumber: Int,
        chunkIndex: Int,
        final: Boolean,
        text: String,
    ): IsolatedPdfParserSessionEvent.ChunkReceived =
        IsolatedPdfParserSessionEvent.ChunkReceived(
            IsolatedPdfParserPageTextChunk(
                pageNumber = pageNumber,
                chunkIndex = chunkIndex,
                isFinalChunk = final,
                text = text,
            ),
        )
}
