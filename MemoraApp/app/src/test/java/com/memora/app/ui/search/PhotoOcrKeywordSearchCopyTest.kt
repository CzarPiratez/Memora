package com.memora.app.ui.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoOcrKeywordSearchCopyTest {
    @Test
    fun scopeAndWhyStayKeywordHonest() {
        assertTrue(PhotoOcrKeywordSearchCopy.SCOPE_BODY.contains("keyword matching"))
        assertTrue(
            PhotoOcrKeywordSearchCopy.whyThisResultBody(
                "total",
                "receipt.jpg",
                "Total 42",
            ).contains("not meaning-based recall"),
        )
        assertFalse(PhotoOcrKeywordSearchCopy.SCOPE_BODY.contains("Memory ranking"))
    }

    @Test
    fun cappedResultsAreExplicit() {
        assertTrue(
            PhotoOcrKeywordSearchCopy.resultsSummary("total", 20, true)
                .contains("at most 20"),
        )
    }
}
