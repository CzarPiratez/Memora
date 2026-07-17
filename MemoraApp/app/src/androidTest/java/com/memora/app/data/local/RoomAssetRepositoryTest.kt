package com.memora.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.IndexingState
import com.memora.app.domain.asset.IndexingStatus
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAssetRepositoryTest {
    private lateinit var database: MemoraDatabase
    private lateinit var repository: RoomAssetRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MemoraDatabase::class.java,
        ).build()
        repository = RoomAssetRepository(database.assetDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savesAndRestoresAnAssetRecord() = runBlocking {
        val record = assetRecord(
            fingerprint = "media:42:1000:2048",
            state = IndexingState.discovered,
        )

        repository.save(record)

        assertEquals(record, repository.find(record.asset.identity))
    }

    @Test
    fun saveUpsertsTheSameSourceIdentityWithoutDuplicatingIt() = runBlocking {
        val identity = AssetIdentity(SourceId("android-media-store"), SourceAssetKey("42"))
        val original = assetRecord(
            identity = identity,
            fingerprint = "media:42:1000:2048",
            state = IndexingState.discovered,
        )
        val updated = assetRecord(
            identity = identity,
            fingerprint = "media:42:2000:4096",
            state = IndexingState.discovered
                .transitionTo(IndexingStatus.EXTRACTION_QUEUED),
        )

        repository.save(original)
        repository.save(updated)

        assertEquals(updated, repository.find(identity))
        assertNull(repository.find(AssetIdentity(SourceId("android-media-store"), SourceAssetKey("missing"))))
    }

    private fun assetRecord(
        identity: AssetIdentity = AssetIdentity(
            SourceId("android-media-store"),
            SourceAssetKey("42"),
        ),
        fingerprint: String,
        state: IndexingState,
    ): AssetIndexRecord = AssetIndexRecord(
        asset = Asset(
            identity = identity,
            type = AssetType.PHOTO,
            location = AssetLocation("content://media/external/images/media/42"),
            fingerprint = AssetFingerprint(fingerprint),
            discoveredAt = Instant.ofEpochMilli(1_720_000_000_000),
            displayName = "Lake.jpg",
            sourceModifiedAt = Instant.ofEpochMilli(1_720_000_000_000),
        ),
        indexingState = state,
    )
}
