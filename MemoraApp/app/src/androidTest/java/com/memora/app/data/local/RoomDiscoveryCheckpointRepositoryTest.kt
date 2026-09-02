package com.memora.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCursor
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
class RoomDiscoveryCheckpointRepositoryTest {
    private lateinit var database: MemoraDatabase
    private lateinit var repository: RoomDiscoveryCheckpointRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MemoraDatabase::class.java,
        ).build()
        repository = RoomDiscoveryCheckpointRepository(
            checkpointDao = { database.discoveryCheckpointDao() },
            clock = Clock.fixed(Instant.parse("2026-07-19T10:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savesAndRestoresASourceOwnedCursor() = runBlocking {
        val cursor = DiscoveryCursor(SourceId("android-media-store-images"), "opaque-v1")

        repository.save(cursor)

        assertEquals(cursor, repository.find(cursor.sourceId))
    }

    @Test
    fun savesTheLatestCursorForTheSameSourceWithoutASecondRecord() = runBlocking {
        val sourceId = SourceId("android-media-store-images")
        repository.save(DiscoveryCursor(sourceId, "first"))
        val latest = DiscoveryCursor(sourceId, "second")

        repository.save(latest)

        assertEquals(latest, repository.find(sourceId))
        assertNull(repository.find(SourceId("user-approved-pdf-folder")))
    }

    @Test
    fun deleteRemovesASavedCheckpoint() = runBlocking {
        val sourceId = SourceId("android-saf-document-tree:test")
        repository.save(DiscoveryCursor(sourceId, "saf-pdf-v2:-"))

        repository.delete(sourceId)

        assertNull(repository.find(sourceId))
    }
}
