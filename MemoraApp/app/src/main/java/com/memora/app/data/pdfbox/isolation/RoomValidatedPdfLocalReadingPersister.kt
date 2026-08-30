package com.memora.app.data.pdfbox.isolation

import com.memora.app.application.documents.PdfLocalReadingStatusCheckResult
import com.memora.app.application.documents.PdfValidatedParsePersistenceHandle
import com.memora.app.application.documents.ValidatedPdfLocalReadingPersister
import com.memora.app.data.local.RoomPdfExtractionPersistencePort
import com.memora.app.data.security.MemoraDatabaseHandle
import com.memora.app.domain.extraction.PdfExtractionRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomValidatedPdfLocalReadingPersister @Inject constructor(
    databaseHandle: MemoraDatabaseHandle,
) : ValidatedPdfLocalReadingPersister {
    private val delegate = PersistValidatedPdfLocalReading(
        persistencePort = RoomPdfExtractionPersistencePort { databaseHandle.database() },
    )

    override suspend fun persist(
        request: PdfExtractionRequest,
        handle: PdfValidatedParsePersistenceHandle,
    ): PdfLocalReadingStatusCheckResult {
        val wire = (handle as? WirePdfValidatedParsePersistenceHandle)?.result
            ?: return PdfLocalReadingStatusCheckResult.RetryableProblem
        return delegate.execute(request, wire)
    }
}
