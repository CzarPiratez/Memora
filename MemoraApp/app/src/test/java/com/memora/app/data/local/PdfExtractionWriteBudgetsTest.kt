package com.memora.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfExtractionWriteBudgetsTest {
    @Test
    fun accepts_within_provisional_envelope() {
        val verdict = PdfExtractionWriteBudgets.evaluate(
            PdfExtractionWriteBudgets.WriteBudgetInspection(
                pageCount = 2,
                totalChars = 100,
                maxCharsOnAnyPage = 50,
                metadataEntryCount = 1,
                maxMetadataNameChars = 6,
                maxMetadataValueChars = 10,
                titleChars = 12,
            ),
        )
        assertEquals(PdfExtractionWriteBudgets.WriteBudgetVerdict.Accepted, verdict)
    }

    @Test
    fun rejects_over_page_count_without_partial_semantics() {
        val verdict = PdfExtractionWriteBudgets.evaluate(
            PdfExtractionWriteBudgets.WriteBudgetInspection(
                pageCount = PdfExtractionWriteBudgets.MAX_PAGE_COUNT + 1,
                totalChars = 0,
                maxCharsOnAnyPage = 0,
                metadataEntryCount = 0,
                maxMetadataNameChars = 0,
                maxMetadataValueChars = 0,
                titleChars = 0,
            ),
        )
        assertTrue(verdict is PdfExtractionWriteBudgets.WriteBudgetVerdict.Rejected)
        assertEquals(
            "page_count",
            (verdict as PdfExtractionWriteBudgets.WriteBudgetVerdict.Rejected).reasonCode,
        )
    }

    @Test
    fun rejects_over_total_chars() {
        val verdict = PdfExtractionWriteBudgets.evaluate(
            PdfExtractionWriteBudgets.WriteBudgetInspection(
                pageCount = 1,
                totalChars = PdfExtractionWriteBudgets.MAX_TOTAL_CHARS + 1,
                maxCharsOnAnyPage = PdfExtractionWriteBudgets.MAX_TOTAL_CHARS + 1,
                metadataEntryCount = 0,
                maxMetadataNameChars = 0,
                maxMetadataValueChars = 0,
                titleChars = 0,
            ),
        )
        assertEquals(
            "total_chars",
            (verdict as PdfExtractionWriteBudgets.WriteBudgetVerdict.Rejected).reasonCode,
        )
    }
}
