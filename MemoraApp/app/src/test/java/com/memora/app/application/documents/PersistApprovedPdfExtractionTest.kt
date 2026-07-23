package com.memora.app.application.documents

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionIntegrity
import com.memora.app.domain.extraction.PdfExtractionPersistenceDecision
import com.memora.app.domain.extraction.PdfExtractionPersistenceFacts
import com.memora.app.domain.extraction.PdfExtractionPersistenceKey
import com.memora.app.domain.extraction.PdfExtractionPersistenceLifecycle
import com.memora.app.domain.extraction.PdfExtractionPersistencePort
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteOutcome
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteRequest
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
import com.memora.app.domain.extraction.PdfExtractionRetryDirective
import com.memora.app.domain.extraction.PdfPageText
import com.memora.app.domain.extraction.PdfTextCoverage
import com.memora.app.domain.extraction.PersistablePdfTextCoverage
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersistApprovedPdfExtractionTest {
    @Test
    fun `eligible record invokes port and reports persisted`() = runBlocking {
        val request = request()
        val port = RecordingPort(PdfExtractionPersistenceWriteOutcome.Persisted)

        val outcome = PersistApprovedPdfExtraction(port).execute(
            decision = eligibleDecision(request),
            record = completeRecord(request),
        )

        assertEquals(PersistApprovedPdfExtractionOutcome.Persisted, outcome)
        assertEquals(request.asset.fingerprint, port.received?.facts?.key?.assetFingerprint)
    }

    @Test
    fun `eligible port retryable failure remains explicit`() = runBlocking {
        val request = request()
        val port = RecordingPort(PdfExtractionPersistenceWriteOutcome.RetryableFailure)

        val outcome = PersistApprovedPdfExtraction(port).execute(eligibleDecision(request), completeRecord(request))

        assertEquals(PersistApprovedPdfExtractionOutcome.RetryableFailure, outcome)
    }

    @Test
    fun `eligible port stale result remains explicit`() = runBlocking {
        val request = request()
        val port = RecordingPort(PdfExtractionPersistenceWriteOutcome.StaleReindexRequired)

        val outcome = PersistApprovedPdfExtraction(port).execute(eligibleDecision(request), completeRecord(request))

        assertEquals(PersistApprovedPdfExtractionOutcome.StaleReindexRequired, outcome)
    }

    @Test
    fun `eligible port safe failure remains explicit`() = runBlocking {
        val request = request()
        val port = RecordingPort(PdfExtractionPersistenceWriteOutcome.FailedSafely)

        val outcome = PersistApprovedPdfExtraction(port).execute(eligibleDecision(request), completeRecord(request))

        assertEquals(PersistApprovedPdfExtractionOutcome.FailedSafely, outcome)
    }

    @Test
    fun `ineligible decision never invokes the port`() = runBlocking {
        val request = request()
        val port = RecordingPort(PdfExtractionPersistenceWriteOutcome.Persisted)
        val decision = PdfExtractionPersistenceDecision.NotEligible(
            key = PdfExtractionPersistenceKey.from(request),
            lifecycle = PdfExtractionPersistenceLifecycle.AWAITING_PERMISSION,
            retry = PdfExtractionRetryDirective.USER_ACTION_REQUIRED,
        )

        val outcome = PersistApprovedPdfExtraction(port).execute(decision, record = null)

        assertEquals(
            PersistApprovedPdfExtractionOutcome.Ineligible(
                lifecycle = PdfExtractionPersistenceLifecycle.AWAITING_PERMISSION,
                retry = PdfExtractionRetryDirective.USER_ACTION_REQUIRED,
            ),
            outcome,
        )
        assertNull(port.received)
    }

    @Test
    fun `eligible decision with no record never invokes the port`() = runBlocking {
        val port = RecordingPort(PdfExtractionPersistenceWriteOutcome.Persisted)

        val outcome = PersistApprovedPdfExtraction(port).execute(eligibleDecision(request()), record = null)

        assertEquals(PersistApprovedPdfExtractionOutcome.InconsistentInput, outcome)
        assertNull(port.received)
    }

    @Test
    fun `eligible decision with a mismatched record never invokes the port`() = runBlocking {
        val request = request()
        val port = RecordingPort(PdfExtractionPersistenceWriteOutcome.Persisted)
        val mismatched = completeRecord(
            PdfExtractionRequest(
                asset = request.asset.copy(fingerprint = AssetFingerprint("fixture:changed")),
                schemaVersion = request.schemaVersion,
            ),
        )

        val outcome = PersistApprovedPdfExtraction(port).execute(eligibleDecision(request), mismatched)

        assertEquals(PersistApprovedPdfExtractionOutcome.InconsistentInput, outcome)
        assertNull(port.received)
    }

    private fun eligibleDecision(
        request: PdfExtractionRequest,
    ): PdfExtractionPersistenceDecision.EligibleForAtomicWrite =
        PdfExtractionPersistenceDecision.EligibleForAtomicWrite(
            PdfExtractionPersistenceFacts(
                key = PdfExtractionPersistenceKey.from(request),
                pageCount = 2,
                coverage = PersistablePdfTextCoverage.COMPLETE,
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
            extractedAt = Instant.parse("2026-07-23T16:00:00Z"),
        )

    private fun request(): PdfExtractionRequest = PdfExtractionRequest(
        asset = Asset(
            identity = AssetIdentity(
                sourceId = SourceId("synthetic-persistence-coordinator-source"),
                sourceAssetKey = SourceAssetKey("fixture:persistence-coordinator.pdf"),
            ),
            type = AssetType.PDF,
            location = AssetLocation("synthetic://persistence-coordinator.pdf"),
            fingerprint = AssetFingerprint("fixture:persistence-coordinator.pdf:42"),
            discoveredAt = Instant.parse("2026-07-23T15:00:00Z"),
        ),
        schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
    )

    private class RecordingPort(
        private val result: PdfExtractionPersistenceWriteOutcome,
    ) : PdfExtractionPersistencePort {
        var received: PdfExtractionPersistenceWriteRequest? = null

        override suspend fun persist(
            request: PdfExtractionPersistenceWriteRequest,
        ): PdfExtractionPersistenceWriteOutcome {
            received = request
            return result
        }
    }
}
