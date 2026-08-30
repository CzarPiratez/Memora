package com.memora.app.application.documents

import android.os.CancellationSignal
import com.memora.app.domain.extraction.PdfExtractionRequest

/**
 * Runs one isolated-parser session for an approved PDF extraction request.
 *
 * Owns service bind/connect lifecycle. Application use cases map outcomes to product
 * status types.
 */
interface PdfIsolatedLocalReadingSession {
    suspend fun parseApprovedRequest(
        request: PdfExtractionRequest,
        cancellationSignal: CancellationSignal?,
    ): PdfIsolatedLocalReadingParseOutcome
}

sealed interface PdfIsolatedLocalReadingParseOutcome {
    data class Parsed(
        val persistenceHandle: PdfValidatedParsePersistenceHandle,
    ) : PdfIsolatedLocalReadingParseOutcome

    data object ServiceUnavailable : PdfIsolatedLocalReadingParseOutcome

    data object AccessStopped : PdfIsolatedLocalReadingParseOutcome

    data object Cancelled : PdfIsolatedLocalReadingParseOutcome

    data object RetryableFailure : PdfIsolatedLocalReadingParseOutcome
}

/** Opaque persistence token for a completed isolated parse (wire details stay in data). */
interface PdfValidatedParsePersistenceHandle
