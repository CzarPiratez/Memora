package com.memora.app.data.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneNoteHtmlPlainTextTest {

    @Test
    fun stripsTagsAndKeepsText() {
        val (text, truncated) = OneNoteHtmlPlainText.toPlainText(
            html = "<html><body><p>Hello&nbsp;<b>world</b></p><script>x()</script></body></html>",
            maxChars = 1_000,
        )
        assertFalse(truncated)
        assertTrue(text.contains("Hello"))
        assertTrue(text.contains("world"))
        assertFalse(text.contains("<"))
        assertFalse(text.contains("script"))
    }

    @Test
    fun truncatesLongText() {
        val (text, truncated) = OneNoteHtmlPlainText.toPlainText(
            html = "<p>" + "a".repeat(100) + "</p>",
            maxChars = 40,
        )
        assertTrue(truncated)
        assertEquals(40, text.length)
    }
}
