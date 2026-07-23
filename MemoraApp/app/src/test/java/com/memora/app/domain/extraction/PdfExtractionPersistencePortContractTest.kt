package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class PdfExtractionPersistencePortContractTest {
    @Test
    fun `eligible complete decision creates one request bound to the same record and facts`() {
        val request = request()
        val record = completeRecord(request)
        val decision = eligibleDecision(request, PersistablePdfTextCoverage.COMPLETE)

        val writeRequest = PdfExtractionPersistenceWriteRequest.from(decision, record)

        assertSame(record, writeRequest.record)
        assertEquals(decision.facts, writeRequest.facts)
    }

    @Test
    fun `eligible no text decision creates one request with explicit no text coverage`() {
        val request = request()
        val record = PdfExtractionRecord.forRequest(
            request = request,
            pageCount = 2,
            textCoverage = PdfTextCoverage.NoExtractableText,
            extractedAt = Instant.parse("2026-07-23T15:00:00Z"),
        )
        val decision = eligibleDecision(request, PersistablePdfTextCoverage.NO_EXTRACTABLE_TEXT)

        val writeRequest = PdfExtractionPersistenceWriteRequest.from(decision, record)

        assertEquals(PersistablePdfTextCoverage.NO_EXTRACTABLE_TEXT, writeRequest.facts.coverage)
    }

    @Test
    fun `mismatched fingerprint cannot create a persistence request`() {
        val request = request()
        val record = completeRecord(
            PdfExtractionRequest(
                asset = request.asset.copy(fingerprint = AssetFingerprint("fixture:changed")),
                schemaVersion = request.schemaVersion,
            ),
        )

        assertRejected { PdfExtractionPersistenceWriteRequest.from(eligibleDecision(request), record) }
    }

    @Test
    fun `partial coverage cannot create a persistence request even from an eligible shaped decision`() {
        val request = request()
        val partial = PdfExtractionRecord.forRequest(
            request = request,
            pageCount = 2,
            pages = listOf(PdfPageText(1, "Only one page.")),
            textCoverage = PdfTextCoverage.Partial("Synthetic interruption."),
            extractedAt = Instant.parse("2026-07-23T15:00:00Z"),
        )

        assertRejected { PdfExtractionPersistenceWriteRequest.from(eligibleDecision(request), partial) }
    }

    @Test
    fun `port returns explicit retryable write outcome for an eligible request`() = runBlocking {
        val request = request()
        val writeRequest = PdfExtractionPersistenceWriteRequest.from(
            eligibleDecision(request),
            completeRecord(request),
        )
        val port = RecordingPort(PdfExtractionPersistenceWriteOutcome.RetryableFailure)

        val outcome = port.persist(writeRequest)

        assertSame(writeRequest, port.received)
        assertEquals(PdfExtractionPersistenceWriteOutcome.RetryableFailure, outcome)
    }

    private fun eligibleDecision(
        request: PdfExtractionRequest,
        coverage: PersistablePdfTextCoverage = PersistablePdfTextCoverage.COMPLETE,
    ): PdfExtractionPersistenceDecision.EligibleForAtomicWrite =
        PdfExtractionPersistenceDecision.EligibleForAtomicWrite(
            PdfExtractionPersistenceFacts(
                key = PdfExtractionPersistenceKey.from(request),
                pageCount = 2,
                coverage = coverage,
                integrity = PdfExtractionIntegrity.VERIFIED,
                retry = PdfExtractionRetryDirective.NO_RETRY_REQUIRED,
            ),
        )

    private fun completeRecord(request: PdfExtractionRequest): PdfExtractionRecord =
        PdfExtractionRecord.forRequest(
            request = request,
            pageCount = 2,
            pages = listOf(PdfPageText(1, "First"), PdfPageText(2, "Second")),
            textCoverage = PdfTextCoverage.Complete,
            extractedAt = Instant.parse("2026-07-23T15:00:00Z"),
        )

    private fun request(): PdfExtractionRequest = PdfExtractionRequest(
        asset = Asset(
            identity = AssetIdentity(
                sourceId = SourceId("synthetic-persistence-port-source"),
                sourceAssetKey = SourceAssetKey("fixture:persistence-port.pdf"),
            ),
            type = AssetType.PDF,
            location = AssetLocation("synthetic://persistence-port.pdf"),
            fingerprint = AssetFingerprint("fixture:persistence-port.pdf:42"),
            discoveredAt = Instant.parse("2026-07-23T14:00:00Z"),
        ),
        schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
    )

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            fail("Expected the persistence request factory to reject inconsistent input.")
        } catch (_: IllegalArgumentException) {
            // Expected: the port may never receive a record/facts mismatch.
        }
    }

    private class RecordingPort(
        private val response: PdfExtractionPersistenceWriteOutcome,
    ) : PdfExtractionPersistencePort {
        var received: PdfExtractionPersistenceWriteRequest? = null

        override suspend fun persist(
            request: PdfExtractionPersistenceWriteRequest,
        ): PdfExtractionPersistenceWriteOutcome {
            received = request
            return response
        }
    }
}
