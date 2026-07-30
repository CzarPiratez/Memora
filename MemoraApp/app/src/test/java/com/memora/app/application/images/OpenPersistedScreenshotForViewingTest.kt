package com.memora.app.application.images

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpenPersistedScreenshotForViewingTest {
    @Test
    fun ready_preview_requires_matching_pixel_buffer() {
        val ready = ScreenshotPreviewRenderResult.Ready(
            screenshotLabel = "Screenshot_memora_note.png",
            widthPx = 2,
            heightPx = 2,
            argb8888 = IntArray(4) { 0xFFFFFFFF.toInt() },
        )
        assertEquals("Screenshot_memora_note.png", ready.screenshotLabel)
        assertEquals(2, ready.widthPx)
    }

    @Test
    fun ready_preview_rejects_mismatched_pixel_count() {
        assertThrows(IllegalArgumentException::class.java) {
            ScreenshotPreviewRenderResult.Ready(
                screenshotLabel = "shot.png",
                widthPx = 2,
                heightPx = 2,
                argb8888 = intArrayOf(0),
            )
        }
    }

    @Test
    fun sample_size_caps_phone_screenshot_under_max_edge() {
        assertEquals(
            4,
            OpenPersistedScreenshotForViewing.computeInSampleSize(1080, 2400, 960),
        )
        assertEquals(
            1,
            OpenPersistedScreenshotForViewing.computeInSampleSize(800, 600, 960),
        )
    }
}
