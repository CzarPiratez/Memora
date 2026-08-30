package com.memora.app.application.documents

import com.memora.app.domain.extraction.PdfExtractionRequest

/** Persists one validated isolated-parser result into searchable extraction storage. */
interface ValidatedPdfLocalReadingPersister {
    suspend fun persist(
        request: PdfExtractionRequest,
        handle: PdfValidatedParsePersistenceHandle,
    ): PdfLocalReadingStatusCheckResult
}
