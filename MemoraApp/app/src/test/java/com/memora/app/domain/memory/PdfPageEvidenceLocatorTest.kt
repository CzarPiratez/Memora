package com.memora.app.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfPageEvidenceLocatorTest {
    @Test
    fun parses_positive_page_numbers() {
        assertEquals(1, PdfPageEvidenceLocator.parsePageNumber("pdf:page:1"))
        assertEquals(12, PdfPageEvidenceLocator.parsePageNumber("pdf:page:12"))
        assertEquals(3, PdfPageEvidenceLocator.parsePageNumber("  pdf:page:3  "))
    }

    @Test
    fun rejects_invalid_locators() {
        assertNull(PdfPageEvidenceLocator.parsePageNumber("pdf:page:0"))
        assertNull(PdfPageEvidenceLocator.parsePageNumber("pdf:page:-1"))
        assertNull(PdfPageEvidenceLocator.parsePageNumber("pdf:title"))
        assertNull(PdfPageEvidenceLocator.parsePageNumber("image:whole"))
        assertNull(PdfPageEvidenceLocator.parsePageNumber("pdf:page:"))
        assertNull(PdfPageEvidenceLocator.parsePageNumber("pdf:page:1a"))
        assertNull(PdfPageEvidenceLocator.parsePageNumber(""))
    }

    @Test
    fun first_page_skips_non_page_locators() {
        assertEquals(
            4,
            PdfPageEvidenceLocator.firstPageNumber(
                listOf("pdf:title", "pdf:page:4", "pdf:page:1"),
            ),
        )
        assertNull(
            PdfPageEvidenceLocator.firstPageNumber(listOf("pdf:title", "image:whole")),
        )
    }
}
