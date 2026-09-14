package com.memora.app.application.images

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpenPersistedPhotoForViewingTest {
    @Test
    fun readyPreviewValidatesPixels() {
        val ready = PhotoPreviewRenderResult.Ready(
            "receipt.jpg",
            2,
            2,
            IntArray(4),
        )
        assertEquals("receipt.jpg", ready.photoLabel)
        assertThrows(IllegalArgumentException::class.java) {
            PhotoPreviewRenderResult.Ready("receipt.jpg", 2, 2, IntArray(1))
        }
    }

    @Test
    fun sampleSizeCapsLargePhoto() {
        assertEquals(8, OpenPersistedPhotoForViewing.computeInSampleSize(4000, 3000, 960))
        assertEquals(2, OpenPersistedPhotoForViewing.computeInSampleSize(4000, 3000, 2048))
        assertEquals(1, OpenPersistedPhotoForViewing.computeInSampleSize(800, 600, 960))
    }
}
