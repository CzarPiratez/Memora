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
import com.memora.app.domain.discovery.DiscoveryCheckpointRepository
import com.memora.app.domain.discovery.DiscoveryCursor
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryPageStore
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import com.memora.app.domain.discovery.SourceAccessState
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IndexMediaStoreImagesTest {
    @Test
    fun reportsFullLibraryAccessAfterOneBoundedPageIsPersisted() = runBlocking {
        val source = FakeImageLibrarySource(
            scope = ImageLibraryAccessScope.FULL_LIBRARY,
            result = DiscoveryResult.Page(page(hasMore = true)),
        )
        val store = RecordingStore()

        val outcome = useCase(source, store)(batchSize = 10)

        assertEquals(
            MediaStoreIndexingOutcome.Indexed(
                discoveredAssetCount = 1,
                hasMore = true,
                accessScope = ImageLibraryAccessScope.FULL_LIBRARY,
            ),
            outcome,
        )
        assertEquals(DiscoveryRequest(batchSize = 10), source.request)
        assertEquals(page(hasMore = true), store.savedPage)
    }

    @Test
    fun reportsSelectedPhotoAccessWithoutCallingItFullLibraryAccess() = runBlocking {
        val source = FakeImageLibrarySource(
            scope = ImageLibraryAccessScope.SELECTED_PHOTOS,
            result = DiscoveryResult.Page(page()),
        )

        assertEquals(
            MediaStoreIndexingOutcome.Indexed(
                discoveredAssetCount = 1,
                hasMore = false,
                accessScope = ImageLibraryAccessScope.SELECTED_PHOTOS,
            ),
            useCase(source)(batchSize = 1),
        )
    }

    @Test
    fun requiresAccessWithoutQueryingOrWriting() = runBlocking {
        val source = FakeImageLibrarySource(scope = null)
        val store = RecordingStore()

        assertEquals(MediaStoreIndexingOutcome.AccessRequired, useCase(source, store)())
        assertNull(source.request)
        assertNull(store.savedPage)
    }

    @Test
    fun preservesExplicitRevocationForALaterViewModel() = runBlocking {
        val source = FakeImageLibrarySource(
            scope = ImageLibraryAccessScope.FULL_LIBRARY,
            accessState = SourceAccessState.ACCESS_REVOKED,
        )

        assertEquals(MediaStoreIndexingOutcome.AccessRevoked, useCase(source)())
        assertNull(source.request)
    }

    @Test
    fun preservesSafeSourceFailuresForALaterViewModel() = runBlocking {
        val failure = DiscoveryFailure("catalogue_busy", "The catalogue is temporarily unavailable.")
        val source = FakeImageLibrarySource(
            scope = ImageLibraryAccessScope.FULL_LIBRARY,
            result = DiscoveryResult.Failed(failure),
        )

        assertEquals(MediaStoreIndexingOutcome.Failed(failure), useCase(source)())
    }

    private fun useCase(
        source: ImageLibraryDiscoverySource,
        store: RecordingStore = RecordingStore(),
    ): IndexMediaStoreImages = IndexMediaStoreImages(
        imageLibrarySource = source,
        discoverSourcePage = DiscoverSourcePage(
            checkpointRepository = EmptyCheckpointRepository,
            processDiscoveryResult = ProcessDiscoveryResult(PersistDiscoveryPage(store)),
        ),
    )

    private object EmptyCheckpointRepository : DiscoveryCheckpointRepository {
        override suspend fun save(cursor: DiscoveryCursor) = Unit

        override suspend fun find(sourceId: SourceId): DiscoveryCursor? = null
    }

    private class RecordingStore : DiscoveryPageStore {
        var savedPage: DiscoveryPage? = null

        override suspend fun save(page: DiscoveryPage) {
            savedPage = page
        }
    }

    private class FakeImageLibrarySource(
        private val scope: ImageLibraryAccessScope?,
        private val accessState: SourceAccessState = SourceAccessState.GRANTED,
        private val result: DiscoveryResult = DiscoveryResult.Page(page()),
    ) : ImageLibraryDiscoverySource {
        override val capability = SourceCapability(
            sourceId = SOURCE_ID,
            supportedAssetTypes = setOf(AssetType.PHOTO, AssetType.SCREENSHOT),
            accessModel = SourceAccessModel.ANDROID_MEDIA_PERMISSION,
            supportsIncrementalDiscovery = true,
            supportsBackgroundIndexing = true,
        )

        var request: DiscoveryRequest? = null

        override suspend fun accessScope(): ImageLibraryAccessScope? = scope

        override suspend fun accessState(): SourceAccessState = accessState

        override suspend fun discover(request: DiscoveryRequest): DiscoveryResult {
            this.request = request
            return result
        }
    }

    private companion object {
        val SOURCE_ID = SourceId("android-media-store-images")

        fun page(hasMore: Boolean = false) = DiscoveryPage(
            sourceId = SOURCE_ID,
            assets = listOf(
                Asset(
                    identity = AssetIdentity(SOURCE_ID, SourceAssetKey("external_primary:42")),
                    type = AssetType.PHOTO,
                    location = AssetLocation("content://media/external_primary/images/media/42"),
                    fingerprint = AssetFingerprint("external_primary:42:9:2048"),
                    discoveredAt = Instant.parse("2026-07-19T10:00:00Z"),
                ),
            ),
            checkpoint = DiscoveryCursor(SOURCE_ID, "after-42"),
            hasMore = hasMore,
        )
    }
}
