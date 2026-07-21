package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfExtractionTest {
    @Test
    fun `complete extraction is bound to its requested asset version and accounts for every page`() {
        val request = request()

        val record = PdfExtractionRecord.forRequest(
            request = request,
            title = "Travel plans",
            pageCount = 2,
            metadata = mapOf("author" to "Mir"),
            pages = listOf(
                PdfPageText(pageNumber = 1, text = "Flights and hotels"),
                PdfPageText(pageNumber = 2, text = "Museum tickets"),
            ),
            textCoverage = PdfTextCoverage.Complete,
            extractedAt = Instant.parse("2026-07-21T10:00:00Z"),
        )

        assertEquals(request.asset.identity, record.assetIdentity)
        assertEquals(request.asset.fingerprint, record.assetFingerprint)
        assertEquals(request.schemaVersion, record.schemaVersion)
        assertEquals(PdfTextCoverage.Complete, record.textCoverage)
    }

    @Test
    fun `pdf extraction rejects a non pdf asset`() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfExtractionRequest(
                asset = asset(type = AssetType.PHOTO),
                schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
            )
        }
    }

    @Test
    fun `complete coverage rejects a missing page`() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfExtractionRecord.forRequest(
                request = request(),
                pageCount = 2,
                pages = listOf(PdfPageText(pageNumber = 1, text = "Only the first page")),
                textCoverage = PdfTextCoverage.Complete,
                extractedAt = Instant.parse("2026-07-21T10:00:00Z"),
            )
        }
    }

    @Test
    fun `partial coverage remains explicit and identifies extracted pages`() {
        val record = PdfExtractionRecord.forRequest(
            request = request(),
            pageCount = 3,
            pages = listOf(PdfPageText(pageNumber = 1, text = "First page only")),
            textCoverage = PdfTextCoverage.Partial("The local read was interrupted."),
            extractedAt = Instant.parse("2026-07-21T10:00:00Z"),
        )

        assertEquals(PdfTextCoverage.Partial("The local read was interrupted."), record.textCoverage)
    }

    @Test
    fun `no extractable text never masquerades as page text`() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfExtractionRecord.forRequest(
                request = request(),
                pageCount = 1,
                pages = listOf(PdfPageText(pageNumber = 1, text = "Invented text")),
                textCoverage = PdfTextCoverage.NoExtractableText,
                extractedAt = Instant.parse("2026-07-21T10:00:00Z"),
            )
        }
    }

    @Test
    fun `failed extraction outcome preserves a meaningful retryable status`() {
        val outcome = PdfExtractionOutcome.Failed(
            message = "The approved PDF could not be read. Try again later.",
            retryable = true,
        )

        assertEquals(true, outcome.retryable)
    }

    private fun request(): PdfExtractionRequest = PdfExtractionRequest(
        asset = asset(type = AssetType.PDF),
        schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
    )

    private fun asset(type: AssetType): Asset = Asset(
        identity = AssetIdentity(
            sourceId = SourceId("approved-pdf-folder"),
            sourceAssetKey = SourceAssetKey("tree:documents:travel.pdf"),
        ),
        type = type,
        location = AssetLocation("content://documents/travel.pdf"),
        fingerprint = AssetFingerprint("travel.pdf:42:2048:application/pdf"),
        discoveredAt = Instant.parse("2026-07-21T09:00:00Z"),
        displayName = "Travel plans.pdf",
    )
}
