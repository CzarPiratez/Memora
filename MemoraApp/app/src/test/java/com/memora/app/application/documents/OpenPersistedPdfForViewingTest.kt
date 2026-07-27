package com.memora.app.application.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpenPersistedPdfForViewingTest {
    @Test
    fun ready_preview_requires_matching_pixel_buffer() {
        val ready = PdfPagePreviewRenderResult.Ready(
            documentLabel = "memora-open-2page.pdf",
            pageNumber = 2,
            pageCount = 2,
            widthPx = 2,
            heightPx = 2,
            argb8888 = IntArray(4) { 0xFFFFFFFF.toInt() },
        )
        assertEquals(2, ready.pageNumber)
        assertEquals(2, ready.pageCount)
    }

    @Test
    fun ready_preview_rejects_page_beyond_count() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfPagePreviewRenderResult.Ready(
                documentLabel = "doc.pdf",
                pageNumber = 3,
                pageCount = 2,
                widthPx = 1,
                heightPx = 1,
                argb8888 = intArrayOf(0),
            )
        }
    }
}
