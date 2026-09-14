package com.memora.app.application.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPreviewScaleTest {
    @Test
    fun thumbnail_edge_is_smaller_than_open_preview() {
        val letter = PdfPreviewScale.scale(612, 792, 128)
        val open = PdfPreviewScale.scale(612, 792, PdfPreviewScale.OPEN_MAX_EDGE_PX)
        assertTrue(letter < open)
        assertEquals(128f / 792f, letter, 0.0001f)
    }

    @Test
    fun does_not_upscale_past_two_and_a_half() {
        assertEquals(2.5f, PdfPreviewScale.scale(10, 10, 1440), 0.0001f)
    }
}
