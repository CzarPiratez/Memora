package com.memora.app.data.pdfbox.isolation

/**
 * Pure, future-facing validator for bounded page-text data returned by the isolated parser.
 *
 * The current Binder service still returns status only. This contract has no Android, Binder,
 * descriptor, source, Room, UI, WorkManager, or AI dependency. Its limits are injected because
 * production values require a separately measured and approved device policy.
 */
internal object IsolatedPdfParserResultContract {
    const val SUPPORTED_SCHEMA_VERSION = 1

    fun validate(
        candidate: IsolatedPdfParserWireResult,
        limits: IsolatedPdfParserResultLimits,
    ): IsolatedPdfParserWireResultValidation {
        if (candidate.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            return rejected()
        }

        return when (candidate.outcome) {
            IsolatedPdfParserWireOutcome.EXTRACTED -> validateExtracted(candidate, limits)
            IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT -> validateNoText(candidate, limits)
            IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED -> validateTerminal(candidate)
            IsolatedPdfParserWireOutcome.FAILURE -> validateFailure(candidate)
        }
    }

    private fun validateExtracted(
        candidate: IsolatedPdfParserWireResult,
        limits: IsolatedPdfParserResultLimits,
    ): IsolatedPdfParserWireResultValidation {
        val pageCount = candidate.pageCount ?: return rejected()
        if (candidate.retryable || pageCount !in 1..limits.maximumPageCount) {
            return rejected()
        }

        val chunksByPage = candidate.chunks.groupBy(IsolatedPdfParserPageTextChunk::pageNumber)
        if (chunksByPage.keys != (1..pageCount).toSet()) {
            return rejected()
        }

        var totalTextCodeUnits = 0L
        for ((pageNumber, chunks) in chunksByPage) {
            if (pageNumber !in 1..pageCount || chunks.size > limits.maximumChunksPerPage) {
                return rejected()
            }
            if (!chunks.areCompleteForOnePage()) {
                return rejected()
            }

            val pageTextCodeUnits = chunks.sumOf { it.text.length.toLong() }
            if (pageTextCodeUnits > limits.maximumPageTextCodeUnits) {
                return rejected()
            }
            totalTextCodeUnits += pageTextCodeUnits
            if (totalTextCodeUnits > limits.maximumTotalTextCodeUnits) {
                return rejected()
            }
        }

        return IsolatedPdfParserWireResultValidation.Valid(candidate)
    }

    private fun validateNoText(
        candidate: IsolatedPdfParserWireResult,
        limits: IsolatedPdfParserResultLimits,
    ): IsolatedPdfParserWireResultValidation {
        val pageCount = candidate.pageCount ?: return rejected()
        return if (
            !candidate.retryable &&
            pageCount in 1..limits.maximumPageCount &&
            candidate.chunks.isEmpty()
        ) {
            IsolatedPdfParserWireResultValidation.Valid(candidate)
        } else {
            rejected()
        }
    }

    private fun validateTerminal(candidate: IsolatedPdfParserWireResult): IsolatedPdfParserWireResultValidation =
        if (!candidate.retryable && candidate.pageCount == null && candidate.chunks.isEmpty()) {
            IsolatedPdfParserWireResultValidation.Valid(candidate)
        } else {
            rejected()
        }

    private fun validateFailure(candidate: IsolatedPdfParserWireResult): IsolatedPdfParserWireResultValidation =
        if (candidate.pageCount == null && candidate.chunks.isEmpty()) {
            IsolatedPdfParserWireResultValidation.Valid(candidate)
        } else {
            rejected()
        }

    private fun List<IsolatedPdfParserPageTextChunk>.areCompleteForOnePage(): Boolean {
        val ordered = sortedBy(IsolatedPdfParserPageTextChunk::chunkIndex)
        return ordered.map(IsolatedPdfParserPageTextChunk::chunkIndex) == ordered.indices.toList() &&
            ordered.count(IsolatedPdfParserPageTextChunk::isFinalChunk) == 1 &&
            ordered.last().isFinalChunk
    }

    private fun rejected(): IsolatedPdfParserWireResultValidation =
        IsolatedPdfParserWireResultValidation.Rejected
}

/** Measured production limits will be supplied later; this type deliberately defines none. */
internal data class IsolatedPdfParserResultLimits(
    val maximumPageCount: Int,
    val maximumChunksPerPage: Int,
    val maximumPageTextCodeUnits: Long,
    val maximumTotalTextCodeUnits: Long,
) {
    init {
        require(maximumPageCount > 0) { "The maximum page count must be positive." }
        require(maximumChunksPerPage > 0) { "The maximum chunks per page must be positive." }
        require(maximumPageTextCodeUnits > 0) { "The maximum page text size must be positive." }
        require(maximumTotalTextCodeUnits > 0) { "The maximum total text size must be positive." }
    }
}

/** Future transport shape only; no Binder codec or service behavior is changed in this step. */
internal data class IsolatedPdfParserWireResult(
    val schemaVersion: Int,
    val outcome: IsolatedPdfParserWireOutcome,
    val retryable: Boolean,
    val pageCount: Int? = null,
    val chunks: List<IsolatedPdfParserPageTextChunk> = emptyList(),
)

/** One bounded fragment of deterministic text from a numbered PDF page. */
internal data class IsolatedPdfParserPageTextChunk(
    val pageNumber: Int,
    val chunkIndex: Int,
    val isFinalChunk: Boolean,
    val text: String,
)

internal enum class IsolatedPdfParserWireOutcome {
    EXTRACTED,
    NO_EXTRACTABLE_TEXT,
    PASSWORD_PROTECTED,
    FAILURE,
}

/** Rejection deliberately carries no candidate text, preventing a partial result from escaping. */
internal sealed interface IsolatedPdfParserWireResultValidation {
    data class Valid(val result: IsolatedPdfParserWireResult) : IsolatedPdfParserWireResultValidation

    data object Rejected : IsolatedPdfParserWireResultValidation
}
