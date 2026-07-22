package com.memora.app.data.pdfbox.isolation

import android.os.Bundle
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.pdfbox.PdfBoxPdfDocumentParser
import com.memora.app.data.pdfbox.PdfDocumentParseResult
import com.memora.app.data.pdfbox.SyntheticPdfBenchmarkCorpus
import com.memora.app.data.pdfbox.SyntheticPdfBenchmarkFixture
import com.memora.app.data.pdfbox.SyntheticPdfFixtures
import com.memora.app.domain.extraction.PdfTextCoverage
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Collects aggregate-only local measurements from repository-owned PDF fixtures.
 *
 * This test neither binds the isolated service nor accesses a descriptor, URI, SAF source, Room,
 * UI, WorkManager, AI, or network. It logs no text or source metadata. Its measurements are a
 * development baseline only and do not establish production parser limits.
 */
@RunWith(AndroidJUnit4::class)
class PdfParserSyntheticBenchmarkIntegrationTest {
    @Before
    fun initializePdfBoxRuntime() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun measures_the_two_page_selectable_fixture_without_logging_its_text() {
        val summary = measureFixture(
            fixtureId = "two_page_selectable",
            fixture = SyntheticPdfFixtures::twoPageSelectable,
        )

        assertEquals(2, summary.pageCount)
        assertTrue(summary.textCodeUnits > 0)
        assertTrue(summary.serializedResultBytes > 0)
        assertTrue(summary.parseElapsedMedianMillis >= 0)
        emitAggregate(summary)
    }

    @Test
    fun measures_the_image_only_fixture_without_logging_its_text() {
        val summary = measureFixture(
            fixtureId = "image_only",
            fixture = SyntheticPdfFixtures::imageOnly,
        )

        assertEquals(1, summary.pageCount)
        assertEquals(0, summary.textCodeUnits)
        assertTrue(summary.serializedResultBytes > 0)
        assertTrue(summary.parseElapsedMedianMillis >= 0)
        emitAggregate(summary)
    }

    @Test
    fun measures_progressively_larger_generated_text_fixtures_without_logging_text() {
        val summaries = SyntheticPdfBenchmarkCorpus.progressivelyLargerTextFixtures().map {
            fixture ->
            val summary = measureFixture(fixture)

            assertEquals(fixture.expectedPageCount, summary.pageCount)
            assertTrue(
                "${fixture.id} must expose at least ${fixture.minimumExpectedTextCodeUnits} " +
                    "text code units, but the parser returned ${summary.textCodeUnits}.",
                summary.textCodeUnits >= fixture.minimumExpectedTextCodeUnits,
            )
            assertTrue(summary.inputPdfBytes > 0)
            assertTrue(summary.serializedResultBytes > 0)
            emitAggregate(summary)
            summary
        }

        assertTrue(
            summaries.zipWithNext().all { (smaller, larger) ->
                smaller.inputPdfBytes < larger.inputPdfBytes &&
                    smaller.textCodeUnits < larger.textCodeUnits &&
                    smaller.serializedResultBytes < larger.serializedResultBytes
            },
        )
    }

    private fun measureFixture(
        fixtureId: String,
        fixture: () -> InputStream,
    ): PdfParserBenchmarkSummary = measurePreparedFixture(
        PreparedPdfBenchmarkFixture(
            id = fixtureId,
            bytes = fixture().use { input -> input.readBytes() },
        ),
    )

    private fun measureFixture(fixture: SyntheticPdfBenchmarkFixture): PdfParserBenchmarkSummary =
        measurePreparedFixture(
            PreparedPdfBenchmarkFixture(
                id = fixture.id,
                bytes = fixture.bytes,
            ),
        )

    private fun measurePreparedFixture(
        fixture: PreparedPdfBenchmarkFixture,
    ): PdfParserBenchmarkSummary {
        measureOnce(fixture) // Warm-up is deliberately not reported.
        val samples = List(MEASURED_RUN_COUNT) { measureOnce(fixture) }
        val first = samples.first()

        assertTrue(samples.all { it.pageCount == first.pageCount })
        assertTrue(samples.all { it.textCodeUnits == first.textCodeUnits })
        assertTrue(samples.all { it.serializedResultBytes == first.serializedResultBytes })

        val elapsedMillis = samples.map { it.parseElapsedNanos / NANOS_PER_MILLISECOND }.sorted()
        return PdfParserBenchmarkSummary(
            fixtureId = fixture.id,
            measuredRunCount = MEASURED_RUN_COUNT,
            inputPdfBytes = fixture.bytes.size,
            pageCount = first.pageCount,
            textCodeUnits = first.textCodeUnits,
            serializedResultBytes = first.serializedResultBytes,
            parseElapsedMinimumMillis = elapsedMillis.first(),
            parseElapsedMedianMillis = elapsedMillis[elapsedMillis.size / 2],
            parseElapsedMaximumMillis = elapsedMillis.last(),
        )
    }

    private fun measureOnce(fixture: PreparedPdfBenchmarkFixture): PdfParserBenchmarkSample {
        val startedAtNanos = SystemClock.elapsedRealtimeNanos()
        val parsed = PdfBoxPdfDocumentParser().parse(ByteArrayInputStream(fixture.bytes))
        val elapsedNanos = SystemClock.elapsedRealtimeNanos() - startedAtNanos
        val result = parsed as? PdfDocumentParseResult.Parsed
            ?: throw AssertionError("The repository-owned benchmark fixture must parse successfully.")

        val textCodeUnits = result.pages.sumOf { it.text.length.toLong() }
        return PdfParserBenchmarkSample(
            pageCount = result.pageCount,
            textCodeUnits = textCodeUnits,
            serializedResultBytes = result.toSyntheticResultBundle().serializedSizeBytes(),
            parseElapsedNanos = elapsedNanos,
        )
    }

    private fun PdfDocumentParseResult.Parsed.toSyntheticResultBundle(): Bundle = Bundle().apply {
        putInt(
            IsolatedPdfParserResultBundleCodec.KEY_SCHEMA_VERSION,
            IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
        )
        putString(
            IsolatedPdfParserResultBundleCodec.KEY_OUTCOME,
            if (textCoverage == PdfTextCoverage.Complete) "extracted" else "no_extractable_text",
        )
        putBoolean(IsolatedPdfParserResultBundleCodec.KEY_RETRYABLE, false)
        putInt(IsolatedPdfParserResultBundleCodec.KEY_PAGE_COUNT, pageCount)
        putParcelableArrayList(
            IsolatedPdfParserResultBundleCodec.KEY_CHUNKS,
            ArrayList(
                pages.map { page ->
                    Bundle().apply {
                        putInt(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_PAGE_NUMBER, page.pageNumber)
                        putInt(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_INDEX, 0)
                        putBoolean(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_IS_FINAL, true)
                        putString(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_TEXT, page.text)
                    }
                },
            ),
        )
    }

    private fun Bundle.serializedSizeBytes(): Int {
        val parcel = Parcel.obtain()
        return try {
            writeToParcel(parcel, 0)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    private fun emitAggregate(summary: PdfParserBenchmarkSummary) {
        Log.i(
            BENCHMARK_LOG_TAG,
            "fixture=${summary.fixtureId}; runs=${summary.measuredRunCount}; " +
                "input_pdf_bytes=${summary.inputPdfBytes}; " +
                "page_count=${summary.pageCount}; text_code_units=${summary.textCodeUnits}; " +
                "result_bundle_bytes=${summary.serializedResultBytes}; " +
                "parse_elapsed_ms_min=${summary.parseElapsedMinimumMillis}; " +
                "parse_elapsed_ms_median=${summary.parseElapsedMedianMillis}; " +
                "parse_elapsed_ms_max=${summary.parseElapsedMaximumMillis}",
        )
    }

    private data class PdfParserBenchmarkSample(
        val pageCount: Int,
        val textCodeUnits: Long,
        val serializedResultBytes: Int,
        val parseElapsedNanos: Long,
    )

    private data class PreparedPdfBenchmarkFixture(
        val id: String,
        val bytes: ByteArray,
    )

    private data class PdfParserBenchmarkSummary(
        val fixtureId: String,
        val measuredRunCount: Int,
        val inputPdfBytes: Int,
        val pageCount: Int,
        val textCodeUnits: Long,
        val serializedResultBytes: Int,
        val parseElapsedMinimumMillis: Long,
        val parseElapsedMedianMillis: Long,
        val parseElapsedMaximumMillis: Long,
    )

    private companion object {
        const val BENCHMARK_LOG_TAG = "MemoraPdfBenchmark"
        const val MEASURED_RUN_COUNT = 5
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
