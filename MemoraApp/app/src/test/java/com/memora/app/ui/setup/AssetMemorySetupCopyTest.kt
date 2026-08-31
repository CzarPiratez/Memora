package com.memora.app.ui.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetMemorySetupCopyTest {
    @Test
    fun `body includes saved OneNote page text and on-device meaning path`() {
        assertTrue(AssetMemorySetupCopy.BODY.contains("OneNote page text"))
        assertTrue(AssetMemorySetupCopy.BODY.contains("on-device model"))
        assertFalse(AssetMemorySetupCopy.BODY.contains("AI understands"))
    }

    @Test
    fun `completed copy explains keyword and meaning search paths`() {
        val completed = AssetMemorySetupCopy.completed(assembledCount = 3, currentReadyCount = 10)
        assertTrue(completed.contains("exact words"))
        assertTrue(completed.contains("on-device model"))
        assertTrue(completed.contains("meaning index"))
        assertFalse(completed.contains("interim recall"))
    }
}
