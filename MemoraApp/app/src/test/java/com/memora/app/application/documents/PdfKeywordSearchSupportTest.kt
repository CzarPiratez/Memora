package com.memora.app.application.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfKeywordSearchSupportTest {
    @Test
    fun normalize_trims_and_collapses_whitespace() {
        assertEquals("hello world", PdfKeywordSearchSupport.normalizeQuery("  hello   world  "))
        assertNull(PdfKeywordSearchSupport.normalizeQuery("   "))
    }

    @Test
    fun normalize_caps_query_length() {
        val long = "a".repeat(PdfKeywordSearchSupport.MAX_QUERY_LENGTH + 40)
        val normalized = requireNotNull(PdfKeywordSearchSupport.normalizeQuery(long))
        assertEquals(PdfKeywordSearchSupport.MAX_QUERY_LENGTH, normalized.length)
    }

    @Test
    fun escape_for_like_treats_wildcards_literally() {
        assertEquals("100\\%\\_done\\\\", PdfKeywordSearchSupport.escapeForLike("100%_done\\"))
    }

    @Test
    fun excerpt_includes_match_and_ellipsis_when_clipped() {
        val page = "AAAA " + "needle" + " BBBB " + "x".repeat(80)
        val excerpt = PdfKeywordSearchSupport.excerptAroundMatch(page, "needle", radius = 8)
        assertTrue(excerpt.contains("needle", ignoreCase = true))
        assertTrue(excerpt.startsWith("…") || excerpt.startsWith("AAAA"))
        assertTrue(excerpt.endsWith("…"))
    }
}
