package com.memora.app.data.pdfbox

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises parser behavior solely against generated, repository-owned input streams.
 *
 * It does not use a ContentResolver, SAF URI, a connected user folder, Room, or the
 * production application database. It creates no source file and makes no network call.
 */
@RunWith(AndroidJUnit4::class)
class PdfBoxPdfDocumentMapperIntegrationTest {
    @Before
    fun initializePdfBoxRuntime() {
        PDFBoxResourceLoader.init(
            ApplicationProvider.getApplicationContext<android.content.Context>(),
        )
    }

    @Test
    fun extractsCompletePageTextMetadataAndARepresentedBlankPage() {
        val stream = TrackingInputStream(SyntheticPdfFixtures.twoPageSelectable())

        val outcome = mapper().extract(request(), stream)

        val record = (outcome as PdfExtractionOutcome.Extracted).record
        assertEquals("Memora synthetic café recall", record.title)
        assertEquals("Memora fixture author", record.metadata["author"])
        assertEquals(2, record.pageCount)
        assertEquals(PdfTextCoverage.Complete, record.textCoverage)
        assertEquals("Café memory: meet Mira at 10:30.\nOnly synthetic text appears in this repository-owned fixture.", record.pages[0].text)
        assertEquals("", record.pages[1].text)
        assertTrue(stream.closed)
    }

    @Test
    fun mapsImageOnlyPdfToExplicitNoTextCoverageWithoutInventingText() {
        val stream = TrackingInputStream(SyntheticPdfFixtures.imageOnly())

        val outcome = mapper().extract(request(), stream)

        val record = (outcome as PdfExtractionOutcome.Extracted).record
        assertEquals(1, record.pageCount)
        assertEquals(PdfTextCoverage.NoExtractableText, record.textCoverage)
        assertTrue(record.pages.isEmpty())
        assertTrue(stream.closed)
    }

    @Test
    fun reportsPasswordProtectionWithoutExposingThePassword() {
        val outcome = mapper().extract(request(), SyntheticPdfFixtures.passwordProtected())

        val failure = outcome as PdfExtractionOutcome.Failed
        assertFalse(failure.retryable)
        assertEquals("This PDF is protected and cannot be read without its password.", failure.message)
        assertFalse(failure.message.contains("synthetic-password"))
    }

    @Test
    fun reportsMalformedInputAsARetryableNonSensitiveFailure() {
        val outcome = mapper().extract(request(), SyntheticPdfFixtures.malformed())

        val failure = outcome as PdfExtractionOutcome.Failed
        assertTrue(failure.retryable)
        assertEquals("Memora could not read this PDF. You can try indexing it again later.", failure.message)
    }

    private fun mapper(): PdfBoxPdfDocumentMapper = PdfBoxPdfDocumentMapper(
        clock = Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC),
    )

    private fun request(): PdfExtractionRequest = PdfExtractionRequest(
        asset = Asset(
            identity = AssetIdentity(
                sourceId = SourceId("synthetic-pdf-fixtures"),
                sourceAssetKey = SourceAssetKey("synthetic:two-page-selectable.pdf"),
            ),
            type = AssetType.PDF,
            location = AssetLocation("synthetic://two-page-selectable.pdf"),
            fingerprint = AssetFingerprint("synthetic-two-page-selectable:v1"),
            discoveredAt = Instant.parse("2026-07-21T11:00:00Z"),
            displayName = "two-page-selectable.pdf",
        ),
        schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
    )

    private class TrackingInputStream(
        private val delegate: InputStream,
    ) : InputStream() {
        var closed = false
            private set

        override fun read(): Int = delegate.read()

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate.read(buffer, offset, length)

        override fun close() {
            closed = true
            delegate.close()
        }
    }
}
