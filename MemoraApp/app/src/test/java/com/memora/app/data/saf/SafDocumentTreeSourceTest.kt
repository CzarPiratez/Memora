package com.memora.app.data.saf

import com.memora.app.domain.discovery.DocumentTreeSource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SafDocumentTreeSourceTest {
    @Test
    fun `the same approved document tree always has the same source identity`() {
        val treeUri = "content://com.android.externalstorage.documents/tree/primary%3ADocuments"

        assertEquals(
            DocumentTreeSource.sourceIdFor(treeUri),
            DocumentTreeSource.sourceIdFor(treeUri),
        )
    }

    @Test
    fun `different approved document trees have independent source identities`() {
        assertFalse(
            DocumentTreeSource.sourceIdFor("content://example/tree/one") ==
                DocumentTreeSource.sourceIdFor("content://example/tree/two"),
        )
    }

    @Test
    fun `approval keeps the private tree reference out of the source identity`() {
        val treeUri = "content://com.android.externalstorage.documents/tree/primary%3ATaxes"

        val approval = DocumentTreeSource.approvalFor(
            persistedTreeUri = treeUri,
            approvedAt = Instant.parse("2026-07-20T12:00:00Z"),
        )

        assertEquals(treeUri, approval.treeUri)
        assertFalse(approval.sourceId.value.contains(treeUri))
    }
}
