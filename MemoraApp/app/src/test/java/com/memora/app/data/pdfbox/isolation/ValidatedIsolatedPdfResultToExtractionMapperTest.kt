package com.memora.app.data.pdfbox.isolation

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionOutcome
import com.memora.app.domain.extraction.PdfExtractionRequest
import com.memora.app.domain.extraction.PdfTextCoverage
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatedIsolatedPdfResultToExtractionMapperTest {
    private val mapper = ValidatedIsolatedPdfResultToExtractionMapper(
        clock = Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC),
    )

    @Test
    fun `maps complete validated chunks into ordered complete page records`() {
        val outcome = mapper.map(
            request(),
            valid(
                outcome = IsolatedPdfParserWireOutcome.EXTRACTED,
                pageCount = 2,
                chunks = listOf(
                    chunk(pageNumber = 2, chunkIndex = 1, final = true, text = "ond"),
                    chunk(pageNumber = 1, chunkIndex = 0, final = true, text = "First"),
                    chunk(pageNumber = 2, chunkIndex = 0, final = false, text = "Sec"),
                ),
            ),
        ) as PdfExtractionOutcome.Extracted

        assertEquals(request().asset.identity, outcome.record.assetIdentity)
        assertEquals(request().asset.fingerprint, outcome.record.assetFingerprint)
        assertEquals(listOf("First", "Second"), outcome.record.pages.map { it.text })
        assertEquals(listOf(1, 2), outcome.record.pages.map { it.pageNumber })
        assertEquals(PdfTextCoverage.Complete, outcome.record.textCoverage)
        assertEquals(Instant.parse("2026-07-23T12:00:00Z"), outcome.record.extractedAt)
    }

    @Test
    fun `maps validated no text result without inventing page text`() {
        val outcome = mapper.map(
            request(),
            valid(
                outcome = IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT,
                pageCount = 2,
            ),
        ) as PdfExtractionOutcome.Extracted

        assertTrue(outcome.record.pages.isEmpty())
        assertEquals(2, outcome.record.pageCount)
        assertEquals(PdfTextCoverage.NoExtractableText, outcome.record.textCoverage)
    }

    @Test
    fun `maps protected result to a non retryable domain failure`() {
        val outcome = mapper.map(
            request(),
            valid(outcome = IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED),
        ) as PdfExtractionOutcome.Failed

        assertFalse(outcome.retryable)
        assertTrue(outcome.message.contains("protected"))
    }

    @Test
    fun `maps validated retryable parser failure without content`() {
        val outcome = mapper.map(
            request(),
            valid(outcome = IsolatedPdfParserWireOutcome.FAILURE, retryable = true),
        ) as PdfExtractionOutcome.Failed

        assertTrue(outcome.retryable)
        assertTrue(outcome.message.contains("try indexing"))
    }

    private fun valid(
        outcome: IsolatedPdfParserWireOutcome,
        retryable: Boolean = false,
        pageCount: Int? = null,
        chunks: List<IsolatedPdfParserPageTextChunk> = emptyList(),
    ): IsolatedPdfParserWireResultValidation.Valid = IsolatedPdfParserWireResultValidation.Valid(
        IsolatedPdfParserWireResult(
            schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
            outcome = outcome,
            retryable = retryable,
            pageCount = pageCount,
            chunks = chunks,
        ),
    )

    private fun chunk(
        pageNumber: Int,
        chunkIndex: Int,
        final: Boolean,
        text: String,
    ): IsolatedPdfParserPageTextChunk = IsolatedPdfParserPageTextChunk(
        pageNumber = pageNumber,
        chunkIndex = chunkIndex,
        isFinalChunk = final,
        text = text,
    )

    private fun request(): PdfExtractionRequest = PdfExtractionRequest(
        asset = Asset(
            identity = AssetIdentity(
                sourceId = SourceId("synthetic-pdf-source"),
                sourceAssetKey = SourceAssetKey("fixture:travel.pdf"),
            ),
            type = AssetType.PDF,
            location = AssetLocation("synthetic://travel.pdf"),
            fingerprint = AssetFingerprint("fixture:travel.pdf:1"),
            discoveredAt = Instant.parse("2026-07-23T11:00:00Z"),
        ),
        schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
    )
}
