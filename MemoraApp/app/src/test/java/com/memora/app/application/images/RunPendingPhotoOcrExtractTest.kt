package com.memora.app.application.images

import android.os.CancellationSignal
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import com.memora.app.domain.discovery.SourceAccessState
import com.memora.app.domain.extraction.PhotoOcrExtractionPersistence
import com.memora.app.domain.extraction.PhotoOcrExtractionRecord
import com.memora.app.domain.extraction.PhotoOcrReadResult
import com.memora.app.domain.extraction.PhotoOcrReader
import com.memora.app.domain.extraction.PhotoOcrSchemaVersion
import com.memora.app.domain.indexing.IndexFailureClass
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunPendingPhotoOcrExtractTest {
    private val sourceId = SourceId("src-photo")
    private val asset = Asset(
        identity = AssetIdentity(sourceId, SourceAssetKey("photo-1")),
        type = AssetType.PHOTO,
        location = AssetLocation("content://media/1"),
        fingerprint = AssetFingerprint("fp-1"),
        discoveredAt = Instant.EPOCH,
    )

    @Test
    fun retryable_failure_records_failure_state_and_returns_outcome() = runBlocking {
        var failureRecorded = false
        var recordedFailureClass: IndexFailureClass? = null

        val fakePersistence = object : PhotoOcrExtractionPersistence {
            override suspend fun findHeader(record: PhotoOcrExtractionRecord): PhotoOcrExtractionRecord? = null
            override suspend fun insert(record: PhotoOcrExtractionRecord) = Unit
            override suspend fun recordFailure(
                asset: Asset,
                schemaVersion: PhotoOcrSchemaVersion,
                engineVersion: String,
                failureClass: IndexFailureClass,
                failureCode: String,
                failureMessage: String,
            ) {
                failureRecorded = true
                recordedFailureClass = failureClass
            }
            override suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int = 0
        }

        val fakeReader = object : PhotoOcrReader {
            override fun read(asset: Asset): PhotoOcrReadResult = PhotoOcrReadResult.RetryableFailure
        }

        val fakeRepo = object : FakeAssetRepositoryBase() {
            override suspend fun findNextPhotoPendingOcrExtract(
                sourceId: SourceId,
                schemaVersion: String,
                afterSourceAssetKey: String?,
            ): Asset? = asset
        }

        val fakeDiscovery = object : ImageLibraryDiscoverySource {
            override val capability get() = error("Not needed")
            override suspend fun accessState() = SourceAccessState.GRANTED
            override suspend fun accessScope() = ImageLibraryAccessScope.FULL_LIBRARY
            override suspend fun discover(request: DiscoveryRequest): DiscoveryResult = error("Not needed")
        }

        val extractor = RunPendingPhotoOcrExtract(
            assetRepository = fakeRepo,
            photoOcrReader = fakeReader,
            imageLibraryDiscoverySource = fakeDiscovery,
            persistence = fakePersistence,
        )

        val outcome = extractor(sourceId, null, CancellationSignal())

        assertEquals(PendingPhotoOcrExtractOutcome.RetryableFailure, outcome)
        assertTrue(failureRecorded)
        assertEquals(IndexFailureClass.RETRYABLE, recordedFailureClass)
    }

    private open class FakeAssetRepositoryBase : AssetRepository {
        override suspend fun save(record: AssetIndexRecord) = Unit
        override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = null
        override suspend fun findFirstBySourceAndType(sourceId: SourceId, type: AssetType): Asset? = null
        override suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int = 0
        override suspend fun findNextPdfPendingLocalReading(sourceId: SourceId, schemaVersion: String, afterSourceAssetKey: String?): Asset? = null
        override suspend fun countPdfPendingLocalReading(sourceId: SourceId, schemaVersion: String): Int = 0
        override suspend fun findNextImagePendingExifExtract(sourceId: SourceId, schemaVersion: String, afterSourceAssetKey: String?): Asset? = null
        override suspend fun findNextScreenshotPendingOcrExtract(sourceId: SourceId, schemaVersion: String, afterSourceAssetKey: String?): Asset? = null
        override suspend fun findNextPhotoPendingOcrExtract(sourceId: SourceId, schemaVersion: String, afterSourceAssetKey: String?): Asset? = null
        override suspend fun findNextNotePendingPageExtract(sourceId: SourceId, schemaVersion: String, afterSourceAssetKey: String?): Asset? = null
    }
}
