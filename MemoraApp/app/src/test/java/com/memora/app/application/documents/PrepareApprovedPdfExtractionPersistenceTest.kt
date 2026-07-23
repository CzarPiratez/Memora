package com.memora.app.application.documents

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
import com.memora.app.domain.extraction.PdfPageText
import com.memora.app.domain.extraction.PdfTextCoverage
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrepareApprovedPdfExtractionPersistenceTest {
    private val policy = PrepareApprovedPdfExtractionPersistence()

    @Test
    fun `complete validated extraction produces content-free facts eligible for an atomic write`() {
        val request = request()

        val decision = policy.prepare(
            request = request,
            outcome = ApprovedPdfExtractionAssemblyOutcome.Extracted(
                completeRecord(request),
            ),
        ) as PdfExtractionPersistenceDecision.EligibleForAtomicWrite

        assertEquals(request.asset.identity, decision.facts.key.assetIdentity)
        assertEquals(request.asset.fingerprint, decision.facts.key.assetFingerprint)
        assertEquals(request.schemaVersion, decision.facts.key.schemaVersion)
        assertEquals(2, decision.facts.pageCount)
        assertEquals(PersistablePdfTextCoverage.COMPLETE, decision.facts.coverage)
        assertEquals(PdfExtractionIntegrity.VERIFIED, decision.facts.integrity)
        assertEquals(PdfExtractionRetryDirective.NO_RETRY_REQUIRED, decision.facts.retry)
        assertEquals(PdfExtractionPersistenceLifecycle.READY, decision.facts.lifecycle)
    }

    @Test
    fun `no text validated extraction is eligible with explicit no text coverage`() {
        val request = request()

        val decision = policy.prepare(
            request = request,
            outcome = ApprovedPdfExtractionAssemblyOutcome.Extracted(
                PdfExtractionRecord.forRequest(
                    request = request,
                    pageCount = 2,
                    textCoverage = PdfTextCoverage.NoExtractableText,
                    extractedAt = Instant.parse("2026-07-23T14:00:00Z"),
                ),
            ),
        ) as PdfExtractionPersistenceDecision.EligibleForAtomicWrite

        assertEquals(PersistablePdfTextCoverage.NO_EXTRACTABLE_TEXT, decision.facts.coverage)
    }

    @Test
    fun `partial extraction is ineligible and requires a fresh extraction`() {
        val request = request()

        val decision = policy.prepare(
            request = request,
            outcome = ApprovedPdfExtractionAssemblyOutcome.Extracted(
                PdfExtractionRecord.forRequest(
                    request = request,
                    pageCount = 2,
                    pages = listOf(PdfPageText(1, "Only the first page.")),
                    textCoverage = PdfTextCoverage.Partial("Synthetic interruption."),
                    extractedAt = Instant.parse("2026-07-23T14:00:00Z"),
                ),
            ),
        ) as PdfExtractionPersistenceDecision.NotEligible

        assertEquals(PdfExtractionPersistenceLifecycle.STALE_REINDEX_REQUIRED, decision.lifecycle)
        assertEquals(PdfExtractionRetryDirective.REQUIRES_FRESH_EXTRACTION, decision.retry)
    }

    @Test
    fun `inconsistent assembly is ineligible and requires a fresh extraction`() {
        val decision = policy.prepare(request(), ApprovedPdfExtractionAssemblyOutcome.InconsistentExtraction)

        assertEquals(
            PdfExtractionPersistenceDecision.NotEligible(
                key = PdfExtractionPersistenceKey.from(request()),
                lifecycle = PdfExtractionPersistenceLifecycle.STALE_REINDEX_REQUIRED,
                retry = PdfExtractionRetryDirective.REQUIRES_FRESH_EXTRACTION,
            ),
            decision,
        )
    }

    @Test
    fun `retryable parser failure keeps a content-free queued retry decision`() {
        val request = request()

        val decision = policy.prepare(request, ApprovedPdfExtractionAssemblyOutcome.RetryableParserFailure)
            as PdfExtractionPersistenceDecision.NotEligible

        assertEquals(PdfExtractionPersistenceLifecycle.QUEUED, decision.lifecycle)
        assertEquals(PdfExtractionRetryDirective.RETRY_WHEN_REQUEUED, decision.retry)
    }

    @Test
    fun `access revocation requires user action and does not become an atomic write`() {
        val request = request()

        val decision = policy.prepare(request, ApprovedPdfExtractionAssemblyOutcome.AccessRevoked)
            as PdfExtractionPersistenceDecision.NotEligible

        assertEquals(PdfExtractionPersistenceLifecycle.AWAITING_PERMISSION, decision.lifecycle)
        assertEquals(PdfExtractionRetryDirective.USER_ACTION_REQUIRED, decision.retry)
    }

    @Test
    fun `non retryable extraction failure remains failed safely`() {
        val request = request()

        val decision = policy.prepare(
            request,
            ApprovedPdfExtractionAssemblyOutcome.ExtractionFailure(retryable = false),
        ) as PdfExtractionPersistenceDecision.NotEligible

        assertEquals(PdfExtractionPersistenceLifecycle.FAILED_SAFELY, decision.lifecycle)
        assertEquals(PdfExtractionRetryDirective.REQUIRES_FRESH_EXTRACTION, decision.retry)
    }

    @Test
    fun `record bound to a different fingerprint is ineligible`() {
        val request = request()
        val differentFingerprint = PdfExtractionRequest(
            asset = request.asset.copy(fingerprint = AssetFingerprint("synthetic:changed")),
            schemaVersion = request.schemaVersion,
        )

        val decision = policy.prepare(
            request,
            ApprovedPdfExtractionAssemblyOutcome.Extracted(completeRecord(differentFingerprint)),
        ) as PdfExtractionPersistenceDecision.NotEligible

        assertEquals(PdfExtractionPersistenceLifecycle.STALE_REINDEX_REQUIRED, decision.lifecycle)
        assertTrue(decision.key.assetFingerprint != differentFingerprint.asset.fingerprint)
    }

    private fun completeRecord(request: PdfExtractionRequest): PdfExtractionRecord =
        PdfExtractionRecord.forRequest(
            request = request,
            pageCount = 2,
            pages = listOf(PdfPageText(1, "First"), PdfPageText(2, "Second")),
            textCoverage = PdfTextCoverage.Complete,
            extractedAt = Instant.parse("2026-07-23T14:00:00Z"),
        )

    private fun request(): PdfExtractionRequest = PdfExtractionRequest(
        asset = Asset(
            identity = AssetIdentity(
                sourceId = SourceId("synthetic-persistence-source"),
                sourceAssetKey = SourceAssetKey("fixture:persistence.pdf"),
            ),
            type = AssetType.PDF,
            location = AssetLocation("synthetic://persistence.pdf"),
            fingerprint = AssetFingerprint("fixture:persistence.pdf:42"),
            discoveredAt = Instant.parse("2026-07-23T13:00:00Z"),
        ),
        schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
    )
}
