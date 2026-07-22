package com.memora.app.data.saf

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistedDocumentTreeGrantMatcherTest {
    private val approvedTree = "content://com.example.documents/tree/reports"

    @Test
    fun `accepts the exact approved tree with retained read access`() {
        assertTrue(
            PersistedDocumentTreeGrantMatcher.hasExactReadGrant(
                approvedTree,
                listOf(PersistedDocumentTreeGrant(approvedTree, hasReadPermission = true)),
            ),
        )
    }

    @Test
    fun `rejects a retained grant for a different tree`() {
        assertFalse(
            PersistedDocumentTreeGrantMatcher.hasExactReadGrant(
                approvedTree,
                listOf(
                    PersistedDocumentTreeGrant(
                        "content://com.example.documents/tree/other",
                        hasReadPermission = true,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `rejects an exact tree grant without read permission`() {
        assertFalse(
            PersistedDocumentTreeGrantMatcher.hasExactReadGrant(
                approvedTree,
                listOf(PersistedDocumentTreeGrant(approvedTree, hasReadPermission = false)),
            ),
        )
    }
}
