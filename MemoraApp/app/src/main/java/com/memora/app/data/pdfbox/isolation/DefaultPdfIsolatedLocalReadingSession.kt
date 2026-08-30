package com.memora.app.data.pdfbox.isolation

import android.content.Context
import android.os.CancellationSignal
import com.memora.app.application.documents.PdfIsolatedLocalReadingParseOutcome
import com.memora.app.application.documents.PdfIsolatedLocalReadingSession
import com.memora.app.application.documents.PdfReadOnlyDescriptorAccess
import com.memora.app.application.documents.PdfReadOnlyDescriptorOutcome
import com.memora.app.domain.extraction.PdfExtractionRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPdfIsolatedLocalReadingSession @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val descriptorAccess: PdfReadOnlyDescriptorAccess,
) : PdfIsolatedLocalReadingSession {
    override suspend fun parseApprovedRequest(
        request: PdfExtractionRequest,
        cancellationSignal: CancellationSignal?,
    ): PdfIsolatedLocalReadingParseOutcome {
        if (cancellationSignal?.isCanceled == true) {
            return PdfIsolatedLocalReadingParseOutcome.Cancelled
        }
        val connection = AndroidIsolatedPdfParserConnection(
            ContextIsolatedPdfParserServiceBinder(context),
        )
        val client = IsolatedPdfParserClient(connection)
        try {
            if (connection.connect() == IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE) {
                return PdfIsolatedLocalReadingParseOutcome.ServiceUnavailable
            }
            if (connection.awaitAvailability(BIND_TIMEOUT_MILLIS) !=
                IsolatedPdfParserBindingStatus.AVAILABLE
            ) {
                return PdfIsolatedLocalReadingParseOutcome.ServiceUnavailable
            }
            if (cancellationSignal?.isCanceled == true) {
                return PdfIsolatedLocalReadingParseOutcome.Cancelled
            }
            val handoff = IsolatedPdfParserDescriptorHandoff(client)
            return when (
                val brokerResult = descriptorAccess.withReadOnlyDescriptor(
                    request = request,
                    cancellationSignal = cancellationSignal,
                ) { descriptor ->
                    handoff.parseBorrowed(descriptor, cancellationSignal)
                }
            ) {
                is PdfReadOnlyDescriptorOutcome.Consumed -> {
                    val wireResult = brokerResult.value
                    PdfIsolatedLocalReadingParseOutcome.Parsed(
                        persistenceHandle = WirePdfValidatedParsePersistenceHandle(wireResult),
                    )
                }
                PdfReadOnlyDescriptorOutcome.AccessRequired,
                PdfReadOnlyDescriptorOutcome.AccessRevoked,
                -> PdfIsolatedLocalReadingParseOutcome.AccessStopped
                PdfReadOnlyDescriptorOutcome.Cancelled ->
                    PdfIsolatedLocalReadingParseOutcome.Cancelled
                PdfReadOnlyDescriptorOutcome.SourceUnavailable,
                PdfReadOnlyDescriptorOutcome.SourceMismatch,
                PdfReadOnlyDescriptorOutcome.StaleSource,
                PdfReadOnlyDescriptorOutcome.InvalidTarget,
                PdfReadOnlyDescriptorOutcome.TreeMembershipDenied,
                PdfReadOnlyDescriptorOutcome.UnsupportedPlatform,
                PdfReadOnlyDescriptorOutcome.RetryableFailure,
                -> PdfIsolatedLocalReadingParseOutcome.RetryableFailure
            }
        } finally {
            client.close()
            connection.close()
        }
    }

    private companion object {
        const val BIND_TIMEOUT_MILLIS = 10_000L
    }
}
