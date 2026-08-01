package com.memora.app.ui.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetMemorySetupCopyTest {
    @Test
    fun `body includes saved OneNote page text without meaning-based ranking`() {
        assertTrue(AssetMemorySetupCopy.BODY.contains("OneNote page text"))
        assertTrue(AssetMemorySetupCopy.BODY.contains("does not provide meaning-based ranking"))
        assertFalse(AssetMemorySetupCopy.BODY.contains("AI understands"))
    }

    @Test
    fun `completed copy keeps keyword search as interim recall path`() {
        val completed = AssetMemorySetupCopy.completed(assembledCount = 3, currentReadyCount = 10)
        assertTrue(completed.contains("note keyword search"))
        assertTrue(completed.contains("interim recall"))
    }
}
