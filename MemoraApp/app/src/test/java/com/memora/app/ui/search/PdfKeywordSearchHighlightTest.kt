package com.memora.app.ui.search

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfKeywordSearchHighlightTest {
    @Test
    fun annotated_excerpt_marks_first_case_insensitive_match() {
        val excerpt = "Café memory: meet Mira at 10:30."
        val query = "meet mira"
        val annotated = PdfKeywordSearchHighlight.annotatedExcerpt(
            excerpt = excerpt,
            query = query,
            highlightColor = Color.Blue,
        )
        assertEquals(excerpt, annotated.text)
        assertEquals(1, annotated.spanStyles.size)
        val span = annotated.spanStyles.single()
        val expectedStart = excerpt.indexOf(query, ignoreCase = true)
        assertEquals(expectedStart, span.start)
        assertEquals(expectedStart + query.length, span.end)
        assertEquals(
            "meet Mira",
            annotated.text.substring(span.start, span.end),
        )
    }

    @Test
    fun annotated_excerpt_stays_plain_when_query_absent() {
        val annotated = PdfKeywordSearchHighlight.annotatedExcerpt(
            excerpt = "no hit here",
            query = "meet mira",
            highlightColor = Color.Blue,
        )
        assertEquals("no hit here", annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
        assertFalse(annotated.text.contains("meet", ignoreCase = true))
    }
}
