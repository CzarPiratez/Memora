package com.memora.app.ui.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginalPreviewZoomCopyTest {
    @Test
    fun zoom_copy_is_on_device_and_not_search_jargon() {
        val copy = listOf(
            OriginalPreviewZoomCopy.ZOOM_IN_LABEL,
            OriginalPreviewZoomCopy.ZOOM_OUT_LABEL,
            OriginalPreviewZoomCopy.FIT_LABEL,
            OriginalPreviewZoomCopy.HINT_BODY,
            OriginalPreviewZoomCopy.SHARPENING_BODY,
        ).joinToString("\n").lowercase()
        assertTrue(copy.contains("zoom in"))
        assertTrue(copy.contains("on this phone"))
        assertTrue(copy.contains("does not upload"))
        assertFalse(copy.contains("embedding"))
        assertFalse(copy.contains("vector"))
        assertFalse(copy.contains("openai"))
        assertFalse(copy.contains("confidence"))
        assertFalse(copy.contains("available"))
    }
}
