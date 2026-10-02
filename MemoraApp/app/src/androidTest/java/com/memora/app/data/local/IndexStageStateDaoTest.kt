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
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.PhotoOcrExtractionRecord
import com.memora.app.domain.extraction.PhotoOcrSchemaVersion
import com.memora.app.domain.indexing.IndexStage
import com.memora.app.domain.indexing.IndexStageStatus
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IndexStageStateDaoTest {
    private lateinit var database: MemoraDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, MemoraDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun upsert_and_find_stage_state() = runBlocking {
        val entity = IndexStageStateEntity(
            sourceId = "src-1",
            sourceAssetKey = "key-1",
            fingerprint = "fp-1",
            stage = IndexStage.OCR.name,
            derivationId = "ocr:v1:16.0.1",
            currentStatus = IndexStageStatus.VALID.name,
            lastAttemptStatus = "SUCCESS",
            lastFailureClass = null,
            lastFailureCode = null,
            lastFailureMessage = null,
            attemptCount = 1,
            runId = null,
            schemaVersion = "v1",
            modelId = null,
            modelVersion = null,
            engineVersion = "16.0.1",
            updatedAtEpochMs = System.currentTimeMillis(),
        )

        database.indexStageStateDao().upsert(entity)

        val found = database.indexStageStateDao().find(
            sourceId = "src-1",
            sourceAssetKey = "key-1",
            fingerprint = "fp-1",
            stage = IndexStage.OCR.name,
            derivationId = "ocr:v1:16.0.1",
        )

        assertNotNull(found)
        assertEquals(IndexStageStatus.VALID.name, found?.currentStatus)
        assertEquals("SUCCESS", found?.lastAttemptStatus)
    }

    @Test
    fun photo_ocr_persistence_port_atomically_commits_ocr_text_and_stage_state() = runBlocking {
        val asset = sampleAsset(sourceId = "src-photo", key = "photo-1", fingerprint = "fp-photo-1")
        database.assetDao().upsert(
            AssetIndexRecord(asset = asset, indexingState = IndexingState.discovered).toEntity(),
        )

        val persistence = RoomPhotoOcrExtractionPersistencePort { database }
        val record = PhotoOcrExtractionRecord(
            asset = asset,
            schemaVersion = PhotoOcrSchemaVersion.V1,
            fullText = "Invoice Total $100.00",
            textTruncated = false,
            engineId = "mlkit-latin",
            engineVersion = "16.0.1",
            extractedAtEpochMillis = System.currentTimeMillis(),
        )

        persistence.insert(record)

        val ocrHeader = persistence.findHeader(record)
        assertNotNull(ocrHeader)
        assertEquals("Invoice Total $100.00", ocrHeader?.fullText)

        val stageState = database.indexStageStateDao().find(
            sourceId = "src-photo",
            sourceAssetKey = "photo-1",
            fingerprint = "fp-photo-1",
            stage = IndexStage.OCR.name,
            derivationId = "ocr:v1:16.0.1",
        )

        assertNotNull(stageState)
        assertEquals(IndexStageStatus.VALID.name, stageState?.currentStatus)
        assertEquals("SUCCESS", stageState?.lastAttemptStatus)
    }

    @Test
    fun stale_worker_with_old_fingerprint_aborts_commit_without_corrupting_state() = runBlocking {
        val newAsset = sampleAsset(sourceId = "src-photo", key = "photo-2", fingerprint = "fp-photo-2-NEW")
        database.assetDao().upsert(
            AssetIndexRecord(asset = newAsset, indexingState = IndexingState.discovered).toEntity(),
        )

        val persistence = RoomPhotoOcrExtractionPersistencePort { database }
        val oldAsset = sampleAsset(sourceId = "src-photo", key = "photo-2", fingerprint = "fp-photo-2-OLD")
        val staleRecord = PhotoOcrExtractionRecord(
            asset = oldAsset,
            schemaVersion = PhotoOcrSchemaVersion.V1,
            fullText = "Old Stale Text",
            textTruncated = false,
            engineId = "mlkit-latin",
            engineVersion = "16.0.1",
            extractedAtEpochMillis = System.currentTimeMillis(),
        )

        persistence.insert(staleRecord)

        val ocrHeader = persistence.findHeader(staleRecord)
        assertNull(ocrHeader)

        val stageState = database.indexStageStateDao().find(
            sourceId = "src-photo",
            sourceAssetKey = "photo-2",
            fingerprint = "fp-photo-2-OLD",
            stage = IndexStage.OCR.name,
            derivationId = "ocr:v1:16.0.1",
        )
        assertNull(stageState)
    }

    @Test
    fun stale_worker_race_condition_aborts_commit_leaving_newer_version_authoritative() = runBlocking {
        // 1. Initial state: Asset V1 is discovered in database
        val v1Asset = sampleAsset(sourceId = "src-photo", key = "photo-race", fingerprint = "fp-V1")
        database.assetDao().upsert(
            AssetIndexRecord(asset = v1Asset, indexingState = IndexingState.discovered).toEntity(),
        )

        // 2. Worker A (V1) reads v1Asset and starts processing...

        // 3. Worker B (V2) arrives due to asset edit and updates database to V2
        val v2Asset = sampleAsset(sourceId = "src-photo", key = "photo-race", fingerprint = "fp-V2-NEW")
        database.assetDao().upsert(
            AssetIndexRecord(asset = v2Asset, indexingState = IndexingState.discovered).toEntity(),
        )

        // 4. Worker A (V1) finishes processing V1 and attempts final persistence operation
        val persistence = RoomPhotoOcrExtractionPersistencePort { database }
        val v1StaleRecord = PhotoOcrExtractionRecord(
            asset = v1Asset,
            schemaVersion = PhotoOcrSchemaVersion.V1,
            fullText = "Stale V1 Text",
            textTruncated = false,
            engineId = "mlkit-latin",
            engineVersion = "16.0.1",
            extractedAtEpochMillis = System.currentTimeMillis(),
        )

        persistence.insert(v1StaleRecord)

        // 5. Verify V1 output was NOT committed
        val v1Header = persistence.findHeader(v1StaleRecord)
        assertNull(v1Header)

        // 6. Verify V1 IndexStageState was NOT committed
        val v1StageState = database.indexStageStateDao().find(
            sourceId = "src-photo",
            sourceAssetKey = "photo-race",
            fingerprint = "fp-V1",
            stage = IndexStage.OCR.name,
            derivationId = "ocr:v1:16.0.1",
        )
        assertNull(v1StageState)

        // 7. Verify V2 remains authoritative in assets table
        val currentDbAsset = database.assetDao().find("src-photo", "photo-race")
        assertEquals("fp-V2-NEW", currentDbAsset?.fingerprint)
    }

    @Test
    fun duplicate_v1_workers_both_succeeding_maintains_consistent_single_valid_state() = runBlocking {
        val v1Asset = sampleAsset(sourceId = "src-photo", key = "photo-dup", fingerprint = "fp-V1")
        database.assetDao().upsert(
            AssetIndexRecord(asset = v1Asset, indexingState = IndexingState.discovered).toEntity(),
        )

        val persistence = RoomPhotoOcrExtractionPersistencePort { database }
        val v1Record = PhotoOcrExtractionRecord(
            asset = v1Asset,
            schemaVersion = PhotoOcrSchemaVersion.V1,
            fullText = "V1 Text",
            textTruncated = false,
            engineId = "mlkit-latin",
            engineVersion = "16.0.1",
            extractedAtEpochMillis = System.currentTimeMillis(),
        )

        // Worker A (V1) completes
        persistence.insert(v1Record)

        // Worker B (V1) completes concurrently/sequentially
        persistence.insert(v1Record)

        // Verify single valid header in photo_ocr_extractions
        val header = persistence.findHeader(v1Record)
        assertNotNull(header)
        assertEquals("V1 Text", header?.fullText)

        // Verify stage state is VALID with attempt count = 2
        val stageState = database.indexStageStateDao().find(
            sourceId = "src-photo",
            sourceAssetKey = "photo-dup",
            fingerprint = "fp-V1",
            stage = IndexStage.OCR.name,
            derivationId = "ocr:v1:16.0.1",
        )

        assertNotNull(stageState)
        assertEquals(IndexStageStatus.VALID.name, stageState?.currentStatus)
        assertEquals("SUCCESS", stageState?.lastAttemptStatus)
        assertEquals(2, stageState?.attemptCount)
    }

    @Test
    fun initial_ocr_failure_records_failed_status() = runBlocking {
        val asset = sampleAsset(sourceId = "src-photo", key = "photo-fail", fingerprint = "fp-fail-1")
        database.assetDao().upsert(
            AssetIndexRecord(asset = asset, indexingState = IndexingState.discovered).toEntity(),
        )

        val persistence = RoomPhotoOcrExtractionPersistencePort { database }
        persistence.recordFailure(
            asset = asset,
            schemaVersion = PhotoOcrSchemaVersion.V1,
            engineVersion = "16.0.1",
            failureClass = com.memora.app.domain.indexing.IndexFailureClass.RETRYABLE,
            failureCode = "OCR_READ_FAILED",
            failureMessage = "Photo OCR read failed for asset",
        )

        val stageState = database.indexStageStateDao().find(
            sourceId = "src-photo",
            sourceAssetKey = "photo-fail",
            fingerprint = "fp-fail-1",
            stage = IndexStage.OCR.name,
            derivationId = "ocr:v1:16.0.1",
        )

        assertNotNull(stageState)
        assertEquals(IndexStageStatus.FAILED.name, stageState?.currentStatus)
        assertEquals("FAILED", stageState?.lastAttemptStatus)
        assertEquals(com.memora.app.domain.indexing.IndexFailureClass.RETRYABLE.name, stageState?.lastFailureClass)
        assertEquals("OCR_READ_FAILED", stageState?.lastFailureCode)
        assertEquals(1, stageState?.attemptCount)
    }

    @Test
    fun valid_output_followed_by_failed_retry_preserves_valid_status_and_output() = runBlocking {
        val asset = sampleAsset(sourceId = "src-photo", key = "photo-retry", fingerprint = "fp-retry-1")
        database.assetDao().upsert(
            AssetIndexRecord(asset = asset, indexingState = IndexingState.discovered).toEntity(),
        )

        val persistence = RoomPhotoOcrExtractionPersistencePort { database }
        val validRecord = PhotoOcrExtractionRecord(
            asset = asset,
            schemaVersion = PhotoOcrSchemaVersion.V1,
            fullText = "Original Valid Text",
            textTruncated = false,
            engineId = "mlkit-latin",
            engineVersion = "16.0.1",
            extractedAtEpochMillis = System.currentTimeMillis(),
        )

        // 1. Initial success
        persistence.insert(validRecord)

        // 2. Retry fails
        persistence.recordFailure(
            asset = asset,
            schemaVersion = PhotoOcrSchemaVersion.V1,
            engineVersion = "16.0.1",
            failureClass = com.memora.app.domain.indexing.IndexFailureClass.RETRYABLE,
            failureCode = "OCR_TIMEOUT",
            failureMessage = "Transient MLKit timeout",
        )

        // Verify valid output was NOT erased
        val ocrHeader = persistence.findHeader(validRecord)
        assertNotNull(ocrHeader)
        assertEquals("Original Valid Text", ocrHeader?.fullText)

        // Verify stage state retains currentStatus = VALID while recording last attempt failure
        val stageState = database.indexStageStateDao().find(
            sourceId = "src-photo",
            sourceAssetKey = "photo-retry",
            fingerprint = "fp-retry-1",
            stage = IndexStage.OCR.name,
            derivationId = "ocr:v1:16.0.1",
        )

        assertNotNull(stageState)
        assertEquals(IndexStageStatus.VALID.name, stageState?.currentStatus)
        assertEquals("FAILED", stageState?.lastAttemptStatus)
        assertEquals(com.memora.app.domain.indexing.IndexFailureClass.RETRYABLE.name, stageState?.lastFailureClass)
        assertEquals("OCR_TIMEOUT", stageState?.lastFailureCode)
        assertEquals(2, stageState?.attemptCount)
    }

    private fun sampleAsset(sourceId: String, key: String, fingerprint: String): Asset = Asset(
        identity = AssetIdentity(SourceId(sourceId), SourceAssetKey(key)),
        type = AssetType.PHOTO,
        location = AssetLocation("content://media/external/images/media/1"),
        fingerprint = AssetFingerprint(fingerprint),
        discoveredAt = Instant.now(),
        displayName = "sample.jpg",
    )
}
