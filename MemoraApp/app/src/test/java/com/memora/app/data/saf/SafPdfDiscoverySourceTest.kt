package com.memora.app.data.saf

import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCursor
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
import com.memora.app.domain.discovery.DocumentTreeSource
import com.memora.app.domain.discovery.SourceAccessState
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafPdfDiscoverySourceTest {
    private val approval = DocumentTreeSource.approvalFor(
        persistedTreeUri = "content://com.example.documents/tree/reports",
        approvedAt = Instant.parse("2026-07-20T00:00:00Z"),
    )
    private val fixedClock = Clock.fixed(Instant.parse("2026-07-20T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `revoked persisted grant is explicit and does not query metadata`() = runTest {
        val catalog = FakeCatalog()
        val accessValidator = FakeAccessValidator(SourceAccessState.ACCESS_REVOKED)
        val source = SafPdfDiscoverySource(
            approval,
            accessValidator,
            catalog,
            fixedClock,
        )

        assertEquals(SourceAccessState.ACCESS_REVOKED, source.accessState())
        assertEquals(DiscoveryResult.AccessRevoked, source.discover(DiscoveryRequest()))
        assertEquals(0, catalog.readCalls)
        assertEquals(2, accessValidator.calls)
    }

    @Test
    fun `maps only declared PDF metadata into a bounded source-owned page`() = runTest {
        val catalog = FakeCatalog(
            page = SafDocumentTreeMetadataPage(
                documents = listOf(
                    document(id = "report", mimeType = "application/pdf", name = "report.pdf"),
                    document(id = "image", mimeType = "image/png", name = "image.png"),
                ),
                hasMore = true,
            ),
        )
        val source = SafPdfDiscoverySource(
            approval,
            FakeAccessValidator(),
            catalog,
            fixedClock,
        )

        val result = source.discover(DiscoveryRequest(batchSize = 2))

        val page = (result as DiscoveryResult.Page).value
        assertEquals(1, page.assets.size)
        assertEquals(approval.sourceId, page.assets.single().identity.sourceId)
        assertEquals("report", page.assets.single().identity.sourceAssetKey.value)
        assertEquals("content://com.example.documents/document/report", page.assets.single().location.value)
        assertEquals("report.pdf", page.assets.single().displayName)
        assertEquals(Instant.parse("2026-07-20T11:00:00Z"), page.assets.single().sourceModifiedAt)
        assertTrue(page.hasMore)
        assertEquals(2, catalog.requestedLimit)
        assertEquals(null, catalog.requestedAfterDocumentId)

        val checkpoint = SafPdfDiscoveryCheckpoint.from(page.checkpoint)
        assertEquals(approval.sourceId, checkpoint.sourceId)
        assertEquals(
            listOf(SafPdfDiscoveryCheckpoint.FolderFrame(parentDocumentId = null, afterDocumentId = "image")),
            checkpoint.frames,
        )
    }

    @Test
    fun `resumes a depth first descendant folder traversal one bounded metadata page at a time`() = runTest {
        val catalog = FakeCatalog(
            pagesByParentDocumentId = mapOf(
                null to SafDocumentTreeMetadataPage(
                    documents = listOf(
                        document(id = "root-pdf", mimeType = "application/pdf", name = "root.pdf"),
                        document(id = "nested", mimeType = DIRECTORY_MIME_TYPE, name = "nested"),
                    ),
                    hasMore = false,
                ),
                "nested" to SafDocumentTreeMetadataPage(
                    documents = listOf(
                        document(id = "nested-pdf", mimeType = "application/pdf", name = "nested.pdf"),
                    ),
                    hasMore = false,
                ),
            ),
        )
        val source = SafPdfDiscoverySource(
            approval,
            FakeAccessValidator(),
            catalog,
            fixedClock,
        )

        val rootResult = source.discover(DiscoveryRequest(batchSize = 2)) as DiscoveryResult.Page
        assertEquals(listOf("root-pdf"), rootResult.value.assets.map { it.identity.sourceAssetKey.value })
        assertTrue(rootResult.value.hasMore)
        assertEquals(
            listOf(SafPdfDiscoveryCheckpoint.FolderFrame(parentDocumentId = "nested", afterDocumentId = null)),
            SafPdfDiscoveryCheckpoint.from(rootResult.value.checkpoint).frames,
        )

        val nestedResult = source.discover(
            DiscoveryRequest(cursor = rootResult.value.checkpoint, batchSize = 2),
        ) as DiscoveryResult.Page
        assertEquals(listOf("nested-pdf"), nestedResult.value.assets.map { it.identity.sourceAssetKey.value })
        assertFalse(nestedResult.value.hasMore)
        assertEquals(emptyList<SafPdfDiscoveryCheckpoint.FolderFrame>(), SafPdfDiscoveryCheckpoint.from(nestedResult.value.checkpoint).frames)
        assertEquals(listOf(null, "nested"), catalog.requestedParentDocumentIds)
    }

    @Test
    fun `resumes a prior root checkpoint without duplicating earlier documents`() = runTest {
        val catalog = FakeCatalog(
            page = SafDocumentTreeMetadataPage(
                documents = listOf(document(id = "later", mimeType = "application/pdf", name = "later.pdf")),
                hasMore = false,
            ),
        )
        val source = SafPdfDiscoverySource(
            approval,
            FakeAccessValidator(),
            catalog,
            fixedClock,
        )
        val legacyCursor = DiscoveryCursor(
            sourceId = approval.sourceId,
            value = "saf-pdf-v1:b2xkZXI",
        )

        val result = source.discover(DiscoveryRequest(cursor = legacyCursor)) as DiscoveryResult.Page

        assertEquals(listOf("later"), result.value.assets.map { it.identity.sourceAssetKey.value })
        assertEquals("older", catalog.requestedAfterDocumentId)
        assertEquals(null, catalog.requestedParentDocumentIds.single())
    }

    @Test
    fun `rejects a cursor belonging to another source without querying metadata`() = runTest {
        val catalog = FakeCatalog()
        val source = SafPdfDiscoverySource(
            approval,
            FakeAccessValidator(),
            catalog,
            fixedClock,
        )
        val foreignCursor = SafPdfDiscoveryCheckpoint(
            sourceId = SourceId("android-saf-document-tree:other"),
            frames = listOf(
                SafPdfDiscoveryCheckpoint.FolderFrame(parentDocumentId = null, afterDocumentId = "report"),
            ),
        ).toCursor()

        val result = source.discover(DiscoveryRequest(cursor = foreignCursor))

        assertEquals("INVALID_SAF_CHECKPOINT", (result as DiscoveryResult.Failed).failure.code)
        assertEquals(0, catalog.readCalls)
    }

    @Test
    fun `catalog failures stay retryable and never look like an empty folder`() = runTest {
        val source = SafPdfDiscoverySource(
            approval = approval,
            accessValidator = FakeAccessValidator(),
            catalog = FakeCatalog(readFailure = IllegalStateException("provider unavailable")),
            clock = fixedClock,
        )

        val result = source.discover(DiscoveryRequest())

        assertEquals("SAF_DOCUMENT_QUERY_FAILED", (result as DiscoveryResult.Failed).failure.code)
    }

    /**
     * A DocumentsProvider is free to ignore the selection, sort and limit hints
     * in a query, and the common ones do. When that happened, page two was page
     * one again: `hasMore` stayed true, the checkpoint came back unchanged, and
     * the discovery worker re-enqueued itself on `hasMore` about twice a second
     * until the battery ran down. [FakeCatalog] ignores `afterDocumentId`
     * exactly as those providers do.
     *
     * A page that claims there is more must have moved.
     */
    @Test
    fun `a provider that ignores the cursor fails instead of scanning forever`() = runTest {
        val catalog = FakeCatalog(
            page = SafDocumentTreeMetadataPage(
                documents = listOf(
                    document(id = "a", mimeType = "application/pdf", name = "a.pdf"),
                    document(id = "b", mimeType = "application/pdf", name = "b.pdf"),
                ),
                hasMore = true,
            ),
        )
        val source = SafPdfDiscoverySource(approval, FakeAccessValidator(), catalog, fixedClock)

        val first = source.discover(DiscoveryRequest(batchSize = 2)) as DiscoveryResult.Page
        assertTrue(first.value.hasMore)

        val second = source.discover(
            DiscoveryRequest(cursor = first.value.checkpoint, batchSize = 2),
        )

        assertEquals("SAF_DOCUMENT_CURSOR_STALLED", (second as DiscoveryResult.Failed).failure.code)
    }

    @Test
    fun `security loss during metadata query is reported as revoked`() = runTest {
        val source = SafPdfDiscoverySource(
            approval = approval,
            accessValidator = FakeAccessValidator(),
            catalog = FakeCatalog(readFailure = SecurityException("grant removed")),
            clock = fixedClock,
        )

        assertEquals(DiscoveryResult.AccessRevoked, source.discover(DiscoveryRequest()))
    }

    private fun document(
        id: String,
        mimeType: String,
        name: String,
    ): SafDocumentMetadata = SafDocumentMetadata(
        documentId = id,
        documentUri = "content://com.example.documents/document/$id",
        mimeType = mimeType,
        displayName = name,
        sizeBytes = 42,
        lastModifiedEpochMillis = Instant.parse("2026-07-20T11:00:00Z").toEpochMilli(),
    )

    private class FakeCatalog(
        private val page: SafDocumentTreeMetadataPage = SafDocumentTreeMetadataPage(emptyList(), false),
        private val pagesByParentDocumentId: Map<String?, SafDocumentTreeMetadataPage> = emptyMap(),
        private val readFailure: Exception? = null,
    ) : SafDocumentTreeCatalog {
        var readCalls: Int = 0
        val requestedParentDocumentIds = mutableListOf<String?>()
        var requestedAfterDocumentId: String? = null
        var requestedLimit: Int? = null

        override suspend fun readChildMetadataPage(
            treeUri: String,
            parentDocumentId: String?,
            afterDocumentId: String?,
            limit: Int,
        ): SafDocumentTreeMetadataPage {
            readCalls += 1
            requestedParentDocumentIds += parentDocumentId
            requestedAfterDocumentId = afterDocumentId
            requestedLimit = limit
            readFailure?.let { throw it }
            return pagesByParentDocumentId[parentDocumentId] ?: page
        }
    }

    private class FakeAccessValidator(
        private val state: SourceAccessState = SourceAccessState.GRANTED,
    ) : DocumentTreeAccessValidator {
        var calls: Int = 0

        override suspend fun accessState(approval: com.memora.app.domain.discovery.DocumentTreeApproval): SourceAccessState {
            calls += 1
            return state
        }
    }

    private companion object {
        const val DIRECTORY_MIME_TYPE = "vnd.android.document/directory"
    }
}
