package com.memora.app.data.pdfbox.isolation

/**
 * Pure ordinary-process assembler for a future session/chunk parser protocol.
 *
 * It has no Android, Binder, descriptor, SAF, Room, UI, WorkManager, or AI dependency.
 * It never returns partial page text on rejection or cancellation. The ordinary-process
 * protocol v3 client feeds Binder session messages into this assembler.
 */
internal class IsolatedPdfParserSessionAssembler(
    private val limits: IsolatedPdfParserSessionLimits,
) {
    private var phase: Phase = Phase.AwaitingOpen

    fun accept(event: IsolatedPdfParserSessionEvent): IsolatedPdfParserSessionState {
        val current = phase
        if (current is Phase.Terminal) {
            return current.state
        }

        return when (event) {
            is IsolatedPdfParserSessionEvent.Opened -> open(event.header)
            is IsolatedPdfParserSessionEvent.ChunkReceived -> receiveChunk(event.chunk)
            IsolatedPdfParserSessionEvent.Completed -> complete()
            IsolatedPdfParserSessionEvent.Cancelled -> cancel()
        }
    }

    private fun open(header: IsolatedPdfParserSessionHeader): IsolatedPdfParserSessionState {
        if (phase !is Phase.AwaitingOpen) {
            return reject()
        }

        return when (header.outcome) {
            IsolatedPdfParserWireOutcome.EXTRACTED -> {
                val pageCount = header.pageCount
                if (
                    header.schemaVersion != IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION ||
                    header.retryable ||
                    pageCount == null ||
                    pageCount !in 1..limits.resultLimits.maximumPageCount
                ) {
                    reject()
                } else {
                    phase = Phase.Collecting(header = header, chunks = emptyList(), totalTextCodeUnits = 0L)
                    IsolatedPdfParserSessionState.Collecting(
                        pageCount = pageCount,
                        receivedChunkCount = 0,
                    )
                }
            }

            IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT,
            IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED,
            IsolatedPdfParserWireOutcome.FAILURE,
            -> finalizeTerminal(header)
        }
    }

    private fun receiveChunk(chunk: IsolatedPdfParserPageTextChunk): IsolatedPdfParserSessionState {
        val collecting = phase as? Phase.Collecting ?: return reject()
        if (collecting.header.outcome != IsolatedPdfParserWireOutcome.EXTRACTED) {
            return reject()
        }

        val pageCount = collecting.header.pageCount ?: return reject()
        if (chunk.pageNumber !in 1..pageCount) {
            return reject()
        }
        if (chunk.text.length.toLong() > limits.maximumChunkTextCodeUnits) {
            return reject()
        }

        val pageChunks = collecting.chunks.filter { it.pageNumber == chunk.pageNumber }
        val expectedIndex = pageChunks.size
        if (chunk.chunkIndex != expectedIndex) {
            return reject()
        }
        if (pageChunks.size >= limits.resultLimits.maximumChunksPerPage) {
            return reject()
        }
        if (pageChunks.any(IsolatedPdfParserPageTextChunk::isFinalChunk)) {
            return reject()
        }

        val pageTextSoFar = pageChunks.sumOf { it.text.length.toLong() } + chunk.text.length.toLong()
        if (pageTextSoFar > limits.resultLimits.maximumPageTextCodeUnits) {
            return reject()
        }

        val totalText = collecting.totalTextCodeUnits + chunk.text.length.toLong()
        if (totalText > limits.resultLimits.maximumTotalTextCodeUnits) {
            return reject()
        }

        val nextChunks = collecting.chunks + chunk
        phase = Phase.Collecting(
            header = collecting.header,
            chunks = nextChunks,
            totalTextCodeUnits = totalText,
        )
        return IsolatedPdfParserSessionState.Collecting(
            pageCount = pageCount,
            receivedChunkCount = nextChunks.size,
        )
    }

    private fun complete(): IsolatedPdfParserSessionState {
        val collecting = phase as? Phase.Collecting ?: return reject()
        val candidate = IsolatedPdfParserWireResult(
            schemaVersion = collecting.header.schemaVersion,
            outcome = collecting.header.outcome,
            retryable = collecting.header.retryable,
            pageCount = collecting.header.pageCount,
            chunks = collecting.chunks,
        )
        return when (val validation = IsolatedPdfParserResultContract.validate(candidate, limits.resultLimits)) {
            is IsolatedPdfParserWireResultValidation.Valid -> {
                phase = Phase.Terminal(IsolatedPdfParserSessionState.Completed(validation.result))
                phase.state()
            }
            IsolatedPdfParserWireResultValidation.Rejected -> reject()
        }
    }

    private fun finalizeTerminal(header: IsolatedPdfParserSessionHeader): IsolatedPdfParserSessionState {
        val candidate = IsolatedPdfParserWireResult(
            schemaVersion = header.schemaVersion,
            outcome = header.outcome,
            retryable = header.retryable,
            pageCount = header.pageCount,
            chunks = emptyList(),
        )
        return when (val validation = IsolatedPdfParserResultContract.validate(candidate, limits.resultLimits)) {
            is IsolatedPdfParserWireResultValidation.Valid -> {
                phase = Phase.Terminal(IsolatedPdfParserSessionState.Completed(validation.result))
                phase.state()
            }
            IsolatedPdfParserWireResultValidation.Rejected -> reject()
        }
    }

    private fun cancel(): IsolatedPdfParserSessionState {
        phase = Phase.Terminal(IsolatedPdfParserSessionState.Cancelled)
        return IsolatedPdfParserSessionState.Cancelled
    }

    private fun reject(): IsolatedPdfParserSessionState {
        phase = Phase.Terminal(IsolatedPdfParserSessionState.Rejected)
        return IsolatedPdfParserSessionState.Rejected
    }

    private fun Phase.state(): IsolatedPdfParserSessionState = when (this) {
        Phase.AwaitingOpen -> IsolatedPdfParserSessionState.AwaitingOpen
        is Phase.Collecting -> IsolatedPdfParserSessionState.Collecting(
            pageCount = header.pageCount ?: 0,
            receivedChunkCount = chunks.size,
        )
        is Phase.Terminal -> state
    }

    private sealed interface Phase {
        data object AwaitingOpen : Phase

        data class Collecting(
            val header: IsolatedPdfParserSessionHeader,
            val chunks: List<IsolatedPdfParserPageTextChunk>,
            val totalTextCodeUnits: Long,
        ) : Phase

        data class Terminal(val state: IsolatedPdfParserSessionState) : Phase
    }
}

/** Injected session ceilings; production values require a separately measured policy. */
internal data class IsolatedPdfParserSessionLimits(
    val resultLimits: IsolatedPdfParserResultLimits,
    val maximumChunkTextCodeUnits: Long,
) {
    init {
        require(maximumChunkTextCodeUnits > 0) { "The maximum chunk text size must be positive." }
    }
}

/** Content-free session header carried before any page text. */
internal data class IsolatedPdfParserSessionHeader(
    val schemaVersion: Int,
    val outcome: IsolatedPdfParserWireOutcome,
    val retryable: Boolean,
    val pageCount: Int? = null,
)

internal sealed interface IsolatedPdfParserSessionEvent {
    data class Opened(val header: IsolatedPdfParserSessionHeader) : IsolatedPdfParserSessionEvent

    data class ChunkReceived(val chunk: IsolatedPdfParserPageTextChunk) : IsolatedPdfParserSessionEvent

    data object Completed : IsolatedPdfParserSessionEvent

    data object Cancelled : IsolatedPdfParserSessionEvent
}

/**
 * Public assembler states never include retained chunk text. Only [Completed] may carry
 * a fully validated wire result for a later governed handoff.
 */
internal sealed interface IsolatedPdfParserSessionState {
    data object AwaitingOpen : IsolatedPdfParserSessionState

    data class Collecting(
        val pageCount: Int,
        val receivedChunkCount: Int,
    ) : IsolatedPdfParserSessionState

    data class Completed(val result: IsolatedPdfParserWireResult) : IsolatedPdfParserSessionState

    data object Rejected : IsolatedPdfParserSessionState

    data object Cancelled : IsolatedPdfParserSessionState
}
