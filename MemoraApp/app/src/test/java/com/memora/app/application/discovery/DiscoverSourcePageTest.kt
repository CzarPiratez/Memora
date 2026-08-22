package com.memora.app.application.discovery

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
import com.memora.app.domain.discovery.SourceAccessState
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverSourcePageTest {
    @Test
    fun resumesWithTheSavedSourceCursorAndCommitsItsPage() = runBlocking {
        val source = RecordingSource(result = DiscoveryResult.Page(page()))
        val checkpoint = DiscoveryCursor(SOURCE_ID, "after-41")
        val store = RecordingStore()

        val result = useCase(checkpoint = checkpoint, store = store)(source, batchSize = 10)

        assertEquals(DiscoveryResult.Page(page()), result)
        assertEquals(DiscoveryRequest(checkpoint, 10), source.request)
        assertEquals(page(), store.savedPage)
    }

    @Test
    fun beginsWithoutACursorWhenTheSourceHasNoSavedCheckpoint() = runBlocking {
        val source = RecordingSource(result = DiscoveryResult.Page(page()))

        useCase()(source)

        assertEquals(DiscoveryRequest(cursor = null), source.request)
    }

    @Test
    fun doesNotQuerySourcesWithoutGrantedAccess() = runBlocking {
        listOf(
            SourceAccessState.ACCESS_REQUIRED to DiscoveryResult.AccessRequired,
            SourceAccessState.ACCESS_REVOKED to DiscoveryResult.AccessRevoked,
            SourceAccessState.UNAVAILABLE to unavailableFailure(),
        ).forEach { (accessState, expectedResult) ->
            val source = RecordingSource(accessState = accessState, result = DiscoveryResult.Page(page()))

            assertEquals(expectedResult, useCase()(source))
            assertNull(source.request)
        }
    }

    @Test
    fun preservesASourceFailureWithoutWritingAPage() = runBlocking {
        val failure = DiscoveryResult.Failed(DiscoveryFailure("temporary", "The source is busy."))
        val store = RecordingStore()

        assertEquals(failure, useCase(store = store)(RecordingSource(result = failure)))
        assertNull(store.savedPage)
    }

    @Test
    fun returnsASafeFailureWhenTheSourceThrows() = runBlocking {
        val source = RecordingSource(throwOnDiscover = true)

        assertEquals(unavailableFailure(), useCase()(source))
    }

    @Test
    fun rejectsAPageFromAnotherSourceWithoutWritingIt() = runBlocking {
        val foreignSourceId = SourceId("foreign-source")
        val store = RecordingStore()
        val source = RecordingSource(result = DiscoveryResult.Page(page(sourceId = foreignSourceId)))

        assertEquals(
            DiscoveryResult.Failed(
                DiscoveryFailure(
                    "discovery_source_id_mismatch",
                    "UNFYND rejected a discovery page from an unexpected source.",
                ),
            ),
            useCase(store = store)(source),
        )
        assertNull(store.savedPage)
    }

    private fun useCase(
        checkpoint: DiscoveryCursor? = null,
        store: RecordingStore = RecordingStore(),
    ) = DiscoverSourcePage(
        checkpointRepository = FakeCheckpointRepository(checkpoint),
        processDiscoveryResult = ProcessDiscoveryResult(PersistDiscoveryPage(store)),
    )

    private class FakeCheckpointRepository(
        private val cursor: DiscoveryCursor?,
    ) : DiscoveryCheckpointRepository {
        override suspend fun save(cursor: DiscoveryCursor) = Unit

        override suspend fun find(sourceId: SourceId): DiscoveryCursor? {
            assertTrue(cursor == null || cursor.sourceId == sourceId)
            return cursor
        }
    }

    private class RecordingStore : DiscoveryPageStore {
        var savedPage: DiscoveryPage? = null

        override suspend fun save(page: DiscoveryPage) {
            savedPage = page
        }
    }

    private class RecordingSource(
        private val accessState: SourceAccessState = SourceAccessState.GRANTED,
        private val result: DiscoveryResult = DiscoveryResult.Failed(
            DiscoveryFailure("not_configured", "No fake source result was configured."),
        ),
        private val throwOnDiscover: Boolean = false,
    ) : AssetDiscoverySource {
        override val capability = SourceCapability(
            sourceId = SOURCE_ID,
            supportedAssetTypes = setOf(AssetType.PHOTO),
            accessModel = SourceAccessModel.ANDROID_MEDIA_PERMISSION,
            supportsIncrementalDiscovery = true,
            supportsBackgroundIndexing = true,
        )

        var request: DiscoveryRequest? = null

        override suspend fun accessState(): SourceAccessState = accessState

        override suspend fun discover(request: DiscoveryRequest): DiscoveryResult {
            this.request = request
            check(!throwOnDiscover) { "Source failure" }
            return result
        }
    }

    private fun page(sourceId: SourceId = SOURCE_ID) = DiscoveryPage(
        sourceId = sourceId,
        assets = listOf(
            Asset(
                identity = AssetIdentity(sourceId, SourceAssetKey("external_primary:42")),
                type = AssetType.PHOTO,
                location = AssetLocation("content://media/external_primary/images/media/42"),
                fingerprint = AssetFingerprint("external_primary:42:9:2048"),
                discoveredAt = Instant.parse("2026-07-19T10:00:00Z"),
            ),
        ),
        checkpoint = DiscoveryCursor(sourceId, "after-42"),
        hasMore = false,
    )

    private fun unavailableFailure() = DiscoveryResult.Failed(
        DiscoveryFailure(
            "discovery_source_unavailable",
            "UNFYND could not read this source. Please try again.",
        ),
    )

    private companion object {
        val SOURCE_ID = SourceId("android-media-store-images")
    }
}
