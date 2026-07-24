package com.memora.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.IndexingState
import com.memora.app.domain.asset.IndexingStatus
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCursor
import com.memora.app.domain.discovery.DiscoveryPage
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDiscoveryPageStoreTest {
    private lateinit var database: MemoraDatabase
    private lateinit var store: RoomDiscoveryPageStore

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MemoraDatabase::class.java,
        ).build()
        store = RoomDiscoveryPageStore(
            database = { database },
            clock = Clock.fixed(Instant.parse("2026-07-19T10:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savesEveryAssetPlaceholderAndItsCheckpointTogether() = runBlocking {
        val page = page(assets = listOf(asset("42"), asset("43")), checkpoint = "after-43")

        store.save(page)

        assertEquals(
            "external_primary:42:9:2048",
            database.assetDao().find(SOURCE_ID.value, "external_primary:42")?.fingerprint,
        )
        assertEquals(
            "external_primary:43:9:2048",
            database.assetDao().find(SOURCE_ID.value, "external_primary:43")?.fingerprint,
        )
        assertEquals("after-43", database.discoveryCheckpointDao().find(SOURCE_ID.value)?.cursorValue)
    }

    @Test
    fun preservesUnchangedStateAndRequeuesChangedAssetVersions() = runBlocking {
        val original = asset("42", fingerprintVersion = 9)
        store.save(page(assets = listOf(original), checkpoint = "first"))
        val queued = database.assetDao().find(SOURCE_ID.value, original.identity.sourceAssetKey.value)!!
            .toDomain()
            .copy(indexingState = IndexingState.discovered.transitionTo(IndexingStatus.EXTRACTION_QUEUED))
        database.assetDao().upsert(queued.toEntity())

        store.save(page(assets = listOf(original.copy(displayName = "renamed.jpg")), checkpoint = "same"))
        assertEquals(
            IndexingStatus.EXTRACTION_QUEUED.name,
            database.assetDao().find(SOURCE_ID.value, original.identity.sourceAssetKey.value)?.indexingStatus,
        )

        store.save(page(assets = listOf(asset("42", fingerprintVersion = 10)), checkpoint = "changed"))
        assertEquals(
            IndexingStatus.DISCOVERED.name,
            database.assetDao().find(SOURCE_ID.value, original.identity.sourceAssetKey.value)?.indexingStatus,
        )
        assertEquals("changed", database.discoveryCheckpointDao().find(SOURCE_ID.value)?.cursorValue)
    }

    @Test
    fun advancesCheckpointForAnEmptyCompletedPage() = runBlocking {
        store.save(page(assets = emptyList(), checkpoint = "completed", hasMore = false))

        assertEquals("completed", database.discoveryCheckpointDao().find(SOURCE_ID.value)?.cursorValue)
        assertNull(database.assetDao().find(SOURCE_ID.value, "external_primary:42"))
    }

    private fun page(
        assets: List<Asset>,
        checkpoint: String,
        hasMore: Boolean = true,
    ) = DiscoveryPage(
        sourceId = SOURCE_ID,
        assets = assets,
        checkpoint = DiscoveryCursor(SOURCE_ID, checkpoint),
        hasMore = hasMore,
    )

    private fun asset(
        mediaId: String,
        fingerprintVersion: Long = 9,
    ) = Asset(
        identity = AssetIdentity(SOURCE_ID, SourceAssetKey("external_primary:$mediaId")),
        type = AssetType.PHOTO,
        location = AssetLocation("content://media/external_primary/images/media/$mediaId"),
        fingerprint = AssetFingerprint("external_primary:$mediaId:$fingerprintVersion:2048"),
        discoveredAt = Instant.parse("2026-07-19T10:00:00Z"),
        displayName = "lake.jpg",
    )

    private companion object {
        val SOURCE_ID = SourceId("android-media-store-images")
    }
}
