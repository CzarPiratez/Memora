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

    @Test
    fun format_locator_is_parseable() {
        assertEquals("pdf:page:7", PdfPageEvidenceLocator.formatLocator(7))
        assertEquals(7, PdfPageEvidenceLocator.parsePageNumber(PdfPageEvidenceLocator.formatLocator(7)))
    }

    @Test
    fun evidence_id_for_page_matches_locator_not_invented() {
        val evidence = listOf(
            MemoryEvidence(
                id = MemoryEvidenceId("e1"),
                kind = MemoryEvidenceKind.SOURCE_METADATA,
                evidenceClass = MemoryEvidenceClass.DIRECT,
                locator = EvidenceLocator("pdf:title"),
                excerpt = MemoryText("Title"),
            ),
            MemoryEvidence(
                id = MemoryEvidenceId("e2"),
                kind = MemoryEvidenceKind.DOCUMENT_TEXT,
                evidenceClass = MemoryEvidenceClass.DIRECT,
                locator = EvidenceLocator("pdf:page:1"),
                excerpt = MemoryText("Page one body"),
            ),
            MemoryEvidence(
                id = MemoryEvidenceId("e3"),
                kind = MemoryEvidenceKind.DOCUMENT_TEXT,
                evidenceClass = MemoryEvidenceClass.DIRECT,
                locator = EvidenceLocator("pdf:page:2"),
                excerpt = MemoryText("Page two body"),
            ),
        )

        assertEquals(MemoryEvidenceId("e2"), PdfPageEvidenceLocator.evidenceIdForPage(evidence, 1))
        assertEquals(MemoryEvidenceId("e3"), PdfPageEvidenceLocator.evidenceIdForPage(evidence, 2))
        assertNull(PdfPageEvidenceLocator.evidenceIdForPage(evidence, 3))
        assertEquals("e2", PdfPageEvidenceLocator.evidenceIdForPage(evidence, 1)!!.value)
        assertNull(PdfPageEvidenceLocator.parsePageNumber("e2"))
    }
}
