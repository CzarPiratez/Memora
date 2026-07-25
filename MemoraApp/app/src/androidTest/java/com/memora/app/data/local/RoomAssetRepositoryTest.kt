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
        repository = RoomAssetRepository { database.assetDao() }
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
    fun findFirstBySourceAndType_returns_first_matching_pdf() = runBlocking {
        val sourceId = SourceId("saf-pdf-folder")
        val laterPdf = assetRecord(
            identity = AssetIdentity(sourceId, SourceAssetKey("b-doc")),
            fingerprint = "pdf:b:2:20",
            type = AssetType.PDF,
            state = IndexingState.discovered,
        )
        val firstPdf = assetRecord(
            identity = AssetIdentity(sourceId, SourceAssetKey("a-doc")),
            fingerprint = "pdf:a:1:10",
            type = AssetType.PDF,
            state = IndexingState.discovered,
        )
        val photo = assetRecord(
            identity = AssetIdentity(sourceId, SourceAssetKey("photo-1")),
            fingerprint = "media:1:1:1",
            type = AssetType.PHOTO,
            state = IndexingState.discovered,
        )

        repository.save(laterPdf)
        repository.save(firstPdf)
        repository.save(photo)

        assertEquals(firstPdf.asset, repository.findFirstBySourceAndType(sourceId, AssetType.PDF))
        assertNull(repository.findFirstBySourceAndType(SourceId("other-source"), AssetType.PDF))
    }

    @Test
    fun findNextPdfPendingLocalReading_skips_current_fingerprint_extractions_and_respects_after_key() =
        runBlocking {
            val sourceId = SourceId("saf-pdf-folder")
            val schema = "pdf-extraction-v1"
            val first = assetRecord(
                identity = AssetIdentity(sourceId, SourceAssetKey("a-doc")),
                fingerprint = "pdf:a:1:10",
                type = AssetType.PDF,
                state = IndexingState.discovered,
            )
            val second = assetRecord(
                identity = AssetIdentity(sourceId, SourceAssetKey("b-doc")),
                fingerprint = "pdf:b:2:20",
                type = AssetType.PDF,
                state = IndexingState.discovered,
            )
            repository.save(first)
            repository.save(second)

            assertEquals(
                first.asset,
                repository.findNextPdfPendingLocalReading(sourceId, schema),
            )

            database.pdfExtractionDao().insertHeader(
                PdfExtractionEntity(
                    sourceId = sourceId.value,
                    sourceAssetKey = "a-doc",
                    fingerprint = "pdf:a:1:10",
                    schemaVersion = schema,
                    pageCount = 1,
                    textCoverage = "FULL",
                    title = null,
                    extractedAtEpochMillis = 1L,
                    createdAtEpochMillis = 1L,
                    integrity = "ok",
                ),
            )

            assertEquals(
                second.asset,
                repository.findNextPdfPendingLocalReading(sourceId, schema),
            )
            assertEquals(
                second.asset,
                repository.findNextPdfPendingLocalReading(
                    sourceId = sourceId,
                    schemaVersion = schema,
                    afterSourceAssetKey = "a-doc",
                ),
            )
            assertNull(
                repository.findNextPdfPendingLocalReading(
                    sourceId = sourceId,
                    schemaVersion = schema,
                    afterSourceAssetKey = "b-doc",
                ),
            )
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
        type: AssetType = AssetType.PHOTO,
        state: IndexingState,
    ): AssetIndexRecord = AssetIndexRecord(
        asset = Asset(
            identity = identity,
            type = type,
            location = AssetLocation("content://media/external/images/media/42"),
            fingerprint = AssetFingerprint(fingerprint),
            discoveredAt = Instant.ofEpochMilli(1_720_000_000_000),
            displayName = "Lake.jpg",
            sourceModifiedAt = Instant.ofEpochMilli(1_720_000_000_000),
        ),
        indexingState = state,
    )
}
