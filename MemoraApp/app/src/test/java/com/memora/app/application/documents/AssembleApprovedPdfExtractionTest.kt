package com.memora.app.application.documents

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionOutcome
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
import com.memora.app.domain.extraction.PdfPageText
import com.memora.app.domain.extraction.PdfTextCoverage
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssembleApprovedPdfExtractionTest {
    @Test
    fun `joins complete parser status with exactly bound complete extraction`() = runBlocking {
        val request = request()
        val outcome = assembler(
            parsing = ApprovedPdfParsingOutcome.Extracted(pageCount = 2),
            extraction = completeExtraction(request),
        ).execute(request)

        val extracted = outcome as ApprovedPdfExtractionAssemblyOutcome.Extracted
        assertEquals(request.asset.identity, extracted.record.assetIdentity)
        assertEquals(PdfTextCoverage.Complete, extracted.record.textCoverage)
        assertEquals(listOf("First", "Second"), extracted.record.pages.map { it.text })
    }

    @Test
    fun `joins no text parser status only with matching no text extraction`() = runBlocking {
        val request = request()
        val outcome = assembler(
            parsing = ApprovedPdfParsingOutcome.NoExtractableText(pageCount = 2),
            extraction = PdfExtractionOutcome.Extracted(
                PdfExtractionRecord.forRequest(
                    request = request,
                    pageCount = 2,
                    textCoverage = PdfTextCoverage.NoExtractableText,
                    extractedAt = Instant.parse("2026-07-23T13:00:00Z"),
                ),
            ),
        ).execute(request)

        assertEquals(
            PdfTextCoverage.NoExtractableText,
            (outcome as ApprovedPdfExtractionAssemblyOutcome.Extracted).record.textCoverage,
        )
    }

    @Test
    fun `does not invoke extraction provider after a retryable parser transport failure`() = runBlocking {
        val calls = mutableListOf<PdfExtractionRequest>()
        val request = request()
        val outcome = AssembleApprovedPdfExtraction(
            parser = ApprovedPdfParsingPort { _, _ -> ApprovedPdfParsingOutcome.RetryableFailure },
            extractionProvider = InMemoryPdfExtractionResultProvider {
                calls += it
                completeExtraction(it)
            },
        ).execute(request)

        assertEquals(ApprovedPdfExtractionAssemblyOutcome.RetryableParserFailure, outcome)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `rejects a successful extraction whose fingerprint does not match the approved request`() = runBlocking {
        val request = request()
        val mismatched = PdfExtractionOutcome.Extracted(
            PdfExtractionRecord.forRequest(
                request = PdfExtractionRequest(
                    asset = request.asset.copy(fingerprint = AssetFingerprint("synthetic:changed")),
                    schemaVersion = request.schemaVersion,
                ),
                pageCount = 2,
                pages = listOf(PdfPageText(1, "First"), PdfPageText(2, "Second")),
                textCoverage = PdfTextCoverage.Complete,
                extractedAt = Instant.parse("2026-07-23T13:00:00Z"),
            ),
        )

        val outcome = assembler(
            parsing = ApprovedPdfParsingOutcome.Extracted(pageCount = 2),
            extraction = mismatched,
        ).execute(request)

        assertEquals(ApprovedPdfExtractionAssemblyOutcome.InconsistentExtraction, outcome)
    }

    @Test
    fun `keeps a post parser extraction failure distinct and retryable`() = runBlocking {
        val request = request()
        val outcome = assembler(
            parsing = ApprovedPdfParsingOutcome.Extracted(pageCount = 2),
            extraction = PdfExtractionOutcome.Failed("Synthetic conversion failed.", retryable = true),
        ).execute(request)

        val failure = outcome as ApprovedPdfExtractionAssemblyOutcome.ExtractionFailure
        assertTrue(failure.retryable)
    }

    @Test
    fun `does not invoke extraction provider after protected parser status`() = runBlocking {
        val calls = mutableListOf<PdfExtractionRequest>()
        val request = request()
        val outcome = AssembleApprovedPdfExtraction(
            parser = ApprovedPdfParsingPort { _, _ -> ApprovedPdfParsingOutcome.PasswordProtected },
            extractionProvider = InMemoryPdfExtractionResultProvider {
                calls += it
                completeExtraction(it)
            },
        ).execute(request)

        assertEquals(ApprovedPdfExtractionAssemblyOutcome.PasswordProtected, outcome)
        assertTrue(calls.isEmpty())
    }

    private fun assembler(
        parsing: ApprovedPdfParsingOutcome,
        extraction: PdfExtractionOutcome,
    ): AssembleApprovedPdfExtraction = AssembleApprovedPdfExtraction(
        parser = ApprovedPdfParsingPort { _, _ -> parsing },
        extractionProvider = InMemoryPdfExtractionResultProvider { extraction },
    )

    private fun completeExtraction(request: PdfExtractionRequest): PdfExtractionOutcome.Extracted =
        PdfExtractionOutcome.Extracted(
            PdfExtractionRecord.forRequest(
                request = request,
                pageCount = 2,
                pages = listOf(PdfPageText(1, "First"), PdfPageText(2, "Second")),
                textCoverage = PdfTextCoverage.Complete,
                extractedAt = Instant.parse("2026-07-23T13:00:00Z"),
            ),
        )

    private fun request(): PdfExtractionRequest = PdfExtractionRequest(
        asset = Asset(
            identity = AssetIdentity(
                sourceId = SourceId("synthetic-approved-source"),
                sourceAssetKey = SourceAssetKey("fixture:approved.pdf"),
            ),
            type = AssetType.PDF,
            location = AssetLocation("synthetic://approved.pdf"),
            fingerprint = AssetFingerprint("fixture:approved.pdf:42"),
            discoveredAt = Instant.parse("2026-07-23T12:00:00Z"),
        ),
        schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
    )
}
