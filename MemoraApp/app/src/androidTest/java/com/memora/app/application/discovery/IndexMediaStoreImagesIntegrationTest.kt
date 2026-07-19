package com.memora.app.application.discovery

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.RoomDiscoveryCheckpointRepository
import com.memora.app.data.local.RoomDiscoveryPageStore
import com.memora.app.data.mediastore.MediaStoreImageDiscoverySource
import com.memora.app.domain.asset.IndexingStatus
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryPageStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the complete read-only MediaStore-to-Room path against the emulator's
 * existing catalogue. The in-memory database is isolated from Memora's normal data;
 * the test does not insert, open, modify, or delete any original media.
 */
@RunWith(AndroidJUnit4::class)
class IndexMediaStoreImagesIntegrationTest {
    private lateinit var database: MemoraDatabase
    private lateinit var source: MediaStoreImageDiscoverySource
    private lateinit var store: RecordingRoomPageStore

    @Before
    fun createDependencies() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, MemoraDatabase::class.java).build()
        source = MediaStoreImageDiscoverySource(context)
        store = RecordingRoomPageStore(RoomDiscoveryPageStore(database))
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun persistsOneLiveBoundedPageAndItsCheckpointInAnIsolatedDatabase() = runBlocking {
        val useCase = IndexMediaStoreImages(
            imageLibrarySource = source,
            discoverSourcePage = DiscoverSourcePage(
                checkpointRepository = RoomDiscoveryCheckpointRepository(database.discoveryCheckpointDao()),
                processDiscoveryResult = ProcessDiscoveryResult(PersistDiscoveryPage(store)),
            ),
        )

        when (val outcome = useCase(batchSize = 2)) {
            is MediaStoreIndexingOutcome.Indexed -> {
                val savedPage = requireNotNull(store.savedPage) {
                    "A successful indexing outcome must persist exactly one discovery page."
                }
                assertTrue(savedPage.assets.size <= 2)
                assertEquals(savedPage.assets.size, outcome.discoveredAssetCount)
                assertEquals(savedPage.hasMore, outcome.hasMore)
                assertEquals(source.accessScope(), outcome.accessScope)
                assertEquals(
                    savedPage.checkpoint.value,
                    database.discoveryCheckpointDao()
                        .find(savedPage.sourceId.value)
                        ?.cursorValue,
                )
                savedPage.assets.forEach { asset ->
                    val savedAsset = database.assetDao().find(
                        asset.identity.sourceId.value,
                        asset.identity.sourceAssetKey.value,
                    )
                    assertNotNull(savedAsset)
                    assertEquals(asset.fingerprint.value, savedAsset?.fingerprint)
                    assertEquals(IndexingStatus.DISCOVERED.name, savedAsset?.indexingStatus)
                }
            }

            MediaStoreIndexingOutcome.AccessRequired -> fail(
                "Grant Memora photo access before running this emulator integration test.",
            )
            MediaStoreIndexingOutcome.AccessRevoked -> fail(
                "The emulator revoked photo access during the MediaStore scan.",
            )
            is MediaStoreIndexingOutcome.Failed -> fail(
                "Unexpected MediaStore indexing failure: ${outcome.failure.code}",
            )
        }
    }

    private class RecordingRoomPageStore(
        private val delegate: RoomDiscoveryPageStore,
    ) : DiscoveryPageStore {
        var savedPage: DiscoveryPage? = null

        override suspend fun save(page: DiscoveryPage) {
            delegate.save(page)
            savedPage = page
        }
    }
}
