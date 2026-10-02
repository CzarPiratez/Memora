package com.memora.app.application.documents

import com.memora.app.application.discovery.DiscoverSourcePage
import com.memora.app.application.discovery.PersistDiscoveryPage
import com.memora.app.application.discovery.ProcessDiscoveryResult
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAccessModel
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceCapability
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.AssetDiscoverySource
import com.memora.app.domain.discovery.DiscoveryCheckpointRepository
import com.memora.app.domain.discovery.DiscoveryCursor
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryPageStore
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.discovery.DocumentTreeSource
import com.memora.app.domain.discovery.PdfFolderDiscoverySourceFactory
import com.memora.app.domain.discovery.SourceAccessState
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexSafPdfFolderTest {
    private val approval = DocumentTreeSource.approvalFor(
        persistedTreeUri = "content://com.example.documents/tree/reports",
        approvedAt = Instant.parse("2026-07-20T00:00:00Z"),
    )

    @Test
    fun persistsOneBoundedPdfPageAndItsSourceOwnedCheckpoint() = runBlocking {
        val source = FakeSource(
            sourceId = approval.sourceId,
            result = DiscoveryResult.Page(
                DiscoveryPage(
                    sourceId = approval.sourceId,
                    assets = listOf(asset("report")),
                    checkpoint = DiscoveryCursor(approval.sourceId, "after-image"),
                    hasMore = true,
                ),
            ),
        )
        val store = RecordingStore()

        val outcome = indexer(source, store)(approval.sourceId, batchSize = 2)

        assertEquals(
            SafPdfFolderIndexingOutcome.Indexed(
                sourceId = approval.sourceId,
                discoveredAssetCount = 1,
                hasMore = true,
            ),
            outcome,
        )
        assertEquals(approval.sourceId, store.savedPage?.sourceId)
        assertEquals(1, store.savedPage?.assets?.size)
        assertEquals(approval.sourceId, store.savedPage?.checkpoint?.sourceId)
        assertEquals(2, source.request?.batchSize)
    }

    @Test
    fun reportsAnUnconnectedSourceWithoutQueryingOrWriting() = runBlocking {
        val source = FakeSource(sourceId = approval.sourceId)
        val store = RecordingStore()

        val outcome = indexer(source, store, repositoryApproval = null)(approval.sourceId)

        assertEquals(SafPdfFolderIndexingOutcome.SourceNotConnected, outcome)
        assertNull(source.request)
        assertNull(store.savedPage)
    }

    @Test
    fun preservesRevokedAccessWithoutWritingAPage() = runBlocking {
        val source = FakeSource(
            sourceId = approval.sourceId,
            accessState = SourceAccessState.ACCESS_REVOKED,
        )
        val store = RecordingStore()

        assertEquals(
            SafPdfFolderIndexingOutcome.AccessRevoked,
            indexer(source, store)(approval.sourceId),
        )
        assertNull(source.request)
        assertNull(store.savedPage)
    }

    @Test
    fun preservesAProviderFailureForALaterViewModel() = runBlocking {
        val failure = DiscoveryFailure(
            code = "SAF_DOCUMENT_QUERY_FAILED",
            message = "UNFYND could not read PDF metadata from the approved folder. You can retry later.",
        )

        val outcome = indexer(
            FakeSource(
                sourceId = approval.sourceId,
                result = DiscoveryResult.Failed(failure),
            ),
        )(approval.sourceId)

        assertEquals(
            SafPdfFolderIndexingOutcome.Failed(failure),
            outcome,
        )
    }

    @Test
    fun clearsACompletedCheckpointBeforeStartingAFreshFolderWalk() = runBlocking {
        val completedCheckpoint = DiscoveryCursor(approval.sourceId, "test-completed-cursor")
        val checkpointRepository = RecordingCheckpointRepository(completedCheckpoint, completed = true)
        val source = FakeSource(
            sourceId = approval.sourceId,
            result = DiscoveryResult.Page(
                DiscoveryPage(
                    sourceId = approval.sourceId,
                    assets = listOf(asset("fresh")),
                    checkpoint = DiscoveryCursor(approval.sourceId, "after-fresh"),
                    hasMore = false,
                ),
            ),
        )

        val outcome = indexer(
            source = source,
            checkpointRepository = checkpointRepository,
        )(approval.sourceId)

        assertTrue(checkpointRepository.deleted)
        assertNull(source.request?.cursor)
        assertEquals(1, (outcome as SafPdfFolderIndexingOutcome.Indexed).discoveredAssetCount)
    }

    private fun indexer(
        source: AssetDiscoverySource,
        store: RecordingStore = RecordingStore(),
        repositoryApproval: DocumentTreeApproval? = approval,
        checkpointRepository: DiscoveryCheckpointRepository = EmptyCheckpointRepository,
    ): IndexSafPdfFolder = IndexSafPdfFolder(
        approvalRepository = RecordingApprovalRepository(repositoryApproval),
        sourceFactory = RecordingFactory(source),
        discoverSourcePage = DiscoverSourcePage(
            checkpointRepository = checkpointRepository,
            processDiscoveryResult = ProcessDiscoveryResult(PersistDiscoveryPage(store)),
        ),
        checkpointRepository = checkpointRepository,
    )

    private fun asset(id: String): Asset = Asset(
        identity = AssetIdentity(approval.sourceId, SourceAssetKey(id)),
        type = AssetType.PDF,
        location = AssetLocation("content://com.example.documents/document/$id"),
        fingerprint = AssetFingerprint("$id:1:42:application/pdf"),
        discoveredAt = Instant.parse("2026-07-20T12:00:00Z"),
    )

    private class RecordingApprovalRepository(
        private val approval: DocumentTreeApproval?,
    ) : DocumentTreeApprovalRepository {
        override suspend fun save(approval: DocumentTreeApproval) = Unit

        override suspend fun find(sourceId: SourceId): DocumentTreeApproval? = approval
            ?.takeIf { candidate -> candidate.sourceId == sourceId }

        override suspend fun findAll(): List<DocumentTreeApproval> = listOfNotNull(approval)
    }

    private class RecordingCheckpointRepository(
        private var cursor: DiscoveryCursor?,
        private val completed: Boolean = false,
    ) : DiscoveryCheckpointRepository {
        var deleted: Boolean = false

        override suspend fun save(cursor: DiscoveryCursor) {
            this.cursor = cursor
        }

        override suspend fun find(sourceId: SourceId): DiscoveryCursor? =
            cursor?.takeIf { it.sourceId == sourceId }

        override suspend fun isCheckpointCompleted(sourceId: SourceId): Boolean = completed

        override suspend fun delete(sourceId: SourceId) {
            if (cursor?.sourceId == sourceId) {
                cursor = null
                deleted = true
            }
        }
    }

    private object EmptyCheckpointRepository : DiscoveryCheckpointRepository {
        override suspend fun save(cursor: DiscoveryCursor) = Unit

        override suspend fun find(sourceId: SourceId): DiscoveryCursor? = null

        override suspend fun delete(sourceId: SourceId) = Unit
    }

    private class RecordingStore : DiscoveryPageStore {
        var savedPage: DiscoveryPage? = null

        override suspend fun save(page: DiscoveryPage) {
            savedPage = page
        }
    }

    private class RecordingFactory(
        private val source: AssetDiscoverySource,
    ) : PdfFolderDiscoverySourceFactory {
        override fun create(approval: DocumentTreeApproval): AssetDiscoverySource = source
    }

    private class FakeSource(
        private val sourceId: SourceId,
        private val accessState: SourceAccessState = SourceAccessState.GRANTED,
        private val result: DiscoveryResult = DiscoveryResult.Page(
            DiscoveryPage(
                sourceId = sourceId,
                assets = emptyList(),
                checkpoint = DiscoveryCursor(sourceId, "initial"),
                hasMore = false,
            ),
        ),
    ) : AssetDiscoverySource {
        override val capability = SourceCapability(
            sourceId = sourceId,
            supportedAssetTypes = setOf(AssetType.PDF),
            accessModel = SourceAccessModel.PERSISTED_DOCUMENT_ACCESS,
            supportsIncrementalDiscovery = true,
            supportsBackgroundIndexing = true,
        )
        var request: DiscoveryRequest? = null

        override suspend fun accessState(): SourceAccessState = accessState

        override suspend fun discover(request: DiscoveryRequest): DiscoveryResult {
            this.request = request
            return result
        }
    }
}
