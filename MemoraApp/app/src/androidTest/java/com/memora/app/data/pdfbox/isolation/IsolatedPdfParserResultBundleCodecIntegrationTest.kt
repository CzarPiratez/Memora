package com.memora.app.data.pdfbox.isolation

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the future result codec with Android Bundles only.
 *
 * It does not bind a service, open a descriptor or source, use a URI, access Room, or expose
 * page text outside synthetic test Bundle values.
 */
@RunWith(AndroidJUnit4::class)
class IsolatedPdfParserResultBundleCodecIntegrationTest {
    @Test
    fun decodes_an_exact_complete_bounded_result() {
        val decoded = IsolatedPdfParserResultBundleCodec.decode(
            extracted(
                pageCount = 2,
                chunks = listOf(
                    chunk(1, 0, true, "first"),
                    chunk(2, 0, true, "second"),
                ),
            ),
            limits(),
        )

        val result = assertValid(decoded)
        assertEquals(IsolatedPdfParserWireOutcome.EXTRACTED, result.outcome)
        assertEquals(2, result.pageCount)
    }

    @Test
    fun decodes_an_exact_no_text_result_without_chunks() {
        val decoded = IsolatedPdfParserResultBundleCodec.decode(
            resultBundle(
                outcome = "no_extractable_text",
                retryable = false,
                pageCount = 1,
                chunks = emptyList(),
            ),
            limits(),
        )

        assertEquals(IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT, assertValid(decoded).outcome)
    }

    @Test
    fun rejects_an_unknown_schema_version() {
        assertRejected(extracted(schemaVersion = 2, pageCount = 1, chunks = listOf(chunk(1, 0, true, "text"))))
    }

    @Test
    fun rejects_a_missing_required_field() {
        val bundle = extracted(pageCount = 1, chunks = listOf(chunk(1, 0, true, "text")))
        bundle.remove(IsolatedPdfParserResultBundleCodec.KEY_RETRYABLE)

        assertRejected(bundle)
    }

    @Test
    fun rejects_an_unknown_top_level_field() {
        val bundle = extracted(pageCount = 1, chunks = listOf(chunk(1, 0, true, "text")))
        bundle.putString("unrecognized", "value")

        assertRejected(bundle)
    }

    @Test
    fun rejects_a_missing_chunk_collection() {
        val bundle = extracted(pageCount = 1, chunks = listOf(chunk(1, 0, true, "text")))
        bundle.remove(IsolatedPdfParserResultBundleCodec.KEY_CHUNKS)

        assertRejected(bundle)
    }

    @Test
    fun rejects_a_malformed_chunk_without_returning_its_text() {
        val chunk = chunk(1, 0, true, "secret")
        chunk.remove(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_TEXT)

        assertRejected(extracted(pageCount = 1, chunks = listOf(chunk)))
    }

    @Test
    fun rejects_output_that_exceeds_the_injected_limit() {
        assertRejected(
            extracted(pageCount = 1, chunks = listOf(chunk(1, 0, true, "too long"))),
            limits(maximumPageTextCodeUnits = 4),
        )
    }

    @Test
    fun rejects_incomplete_page_coverage_through_the_pure_contract() {
        assertRejected(extracted(pageCount = 2, chunks = listOf(chunk(1, 0, true, "only one"))))
    }

    @Test
    fun rejects_failure_output_that_carries_page_data() {
        assertRejected(
            resultBundle(
                outcome = "failure",
                retryable = true,
                pageCount = 1,
                chunks = emptyList(),
            ),
        )
    }

    private fun extracted(
        schemaVersion: Int = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
        pageCount: Int,
        chunks: List<Bundle>,
    ): Bundle = resultBundle(
        schemaVersion = schemaVersion,
        outcome = "extracted",
        retryable = false,
        pageCount = pageCount,
        chunks = chunks,
    )

    private fun resultBundle(
        schemaVersion: Int = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
        outcome: String,
        retryable: Boolean,
        pageCount: Int?,
        chunks: List<Bundle>,
    ): Bundle = Bundle().apply {
        putInt(IsolatedPdfParserResultBundleCodec.KEY_SCHEMA_VERSION, schemaVersion)
        putString(IsolatedPdfParserResultBundleCodec.KEY_OUTCOME, outcome)
        putBoolean(IsolatedPdfParserResultBundleCodec.KEY_RETRYABLE, retryable)
        pageCount?.let { putInt(IsolatedPdfParserResultBundleCodec.KEY_PAGE_COUNT, it) }
        putParcelableArrayList(
            IsolatedPdfParserResultBundleCodec.KEY_CHUNKS,
            ArrayList(chunks),
        )
    }

    private fun chunk(
        pageNumber: Int,
        index: Int,
        final: Boolean,
        text: String,
    ): Bundle = Bundle().apply {
        putInt(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_PAGE_NUMBER, pageNumber)
        putInt(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_INDEX, index)
        putBoolean(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_IS_FINAL, final)
        putString(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_TEXT, text)
    }

    private fun limits(
        maximumPageTextCodeUnits: Long = 10,
    ): IsolatedPdfParserResultLimits = IsolatedPdfParserResultLimits(
        maximumPageCount = 2,
        maximumChunksPerPage = 2,
        maximumPageTextCodeUnits = maximumPageTextCodeUnits,
        maximumTotalTextCodeUnits = 12,
    )

    private fun assertValid(
        decoded: IsolatedPdfParserWireResultValidation,
    ): IsolatedPdfParserWireResult = (decoded as? IsolatedPdfParserWireResultValidation.Valid)?.result
        ?: throw AssertionError("Expected a valid bounded parser result.")

    private fun assertRejected(
        bundle: Bundle,
        limits: IsolatedPdfParserResultLimits = limits(),
    ) {
        assertEquals(
            IsolatedPdfParserWireResultValidation.Rejected,
            IsolatedPdfParserResultBundleCodec.decode(bundle, limits),
        )
    }
}
