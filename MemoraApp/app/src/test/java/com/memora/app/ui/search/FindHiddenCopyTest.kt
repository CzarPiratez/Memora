package com.memora.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindHiddenCopyTest {
    @Test
    fun hide_is_a_visibility_preference_not_erase() {
        val copy = listOf(
            FindHiddenCopy.HIDE_LABEL,
            FindHiddenCopy.SHOW_AGAIN_LABEL,
            FindHiddenCopy.HIDDEN_NOTICE,
            FindHiddenCopy.ALL_HIDDEN_BODY,
            FindHiddenCopy.panelTitle(1),
            FindHiddenCopy.panelTitle(3),
        ).joinToString("\n").lowercase()

        assertTrue(copy.contains("hide from find"))
        assertTrue(copy.contains("show again"))
        assertTrue(copy.contains("hidden from this search"))
        assertTrue(copy.contains("memory is still on this phone"))
        assertFalse(copy.contains("deleted the memory"))
        assertFalse(copy.contains("cr-08"))
        assertFalse(copy.contains("erase"))
        assertFalse(copy.contains("unavailable"))
        assertFalse(copy.contains("openai"))
        assertFalse(FindHiddenCopy.ALL_HIDDEN_BODY.lowercase().contains("no matches"))
        assertFalse(FindHiddenCopy.HIDDEN_NOTICE.contains("."))
        assertEquals("1 hidden from this search", FindHiddenCopy.panelTitle(1))
        assertEquals("3 hidden from this search", FindHiddenCopy.panelTitle(3))
    }
}
