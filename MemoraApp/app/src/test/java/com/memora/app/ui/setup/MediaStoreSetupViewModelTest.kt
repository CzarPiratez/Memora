package com.memora.app.ui.setup

import androidx.work.Data
import androidx.work.WorkInfo
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import com.memora.app.domain.extraction.ImageExifExtractionPersistence
import com.memora.app.domain.extraction.ImageExifExtractionRecord
import com.memora.app.domain.extraction.ScreenshotOcrExtractionPersistence
import com.memora.app.domain.extraction.ScreenshotOcrExtractionRecord
import com.memora.app.work.MediaStoreDiscoveryWorkScheduler
import com.memora.app.work.MediaStoreDiscoveryWorker
import com.memora.app.work.MediaStoreImageExifExtractWorkScheduler
import com.memora.app.work.MediaStoreScreenshotOcrExtractWorkScheduler
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaStoreSetupViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun doesNotIndexUntilTheUiReportsGrantedAccessAndTheUserExplicitlyRequestsIt() = runTest {
        val scheduler = RecordingScheduler()
        val viewModel = viewModel(discovery = scheduler)

        viewModel.onIndexRequested()
        assertEquals(0, scheduler.drainCount)
        assertEquals(MediaStoreSetupUiState(), viewModel.uiState.value)

        viewModel.onPhotoPermissionResult(isGranted = true)
        assertEquals(0, scheduler.drainCount)
        assertEquals(PhotoAccessState.GRANTED, viewModel.uiState.value.photoAccess)
        assertEquals(MediaStoreIndexingState.NOT_STARTED, viewModel.uiState.value.indexing)

        viewModel.onIndexRequested()
        assertEquals(MediaStoreIndexingState.IN_PROGRESS, viewModel.uiState.value.indexing)
        assertEquals(1, scheduler.drainCount)

        scheduler.emit(
            listOf(
                workInfo(
                    state = WorkInfo.State.SUCCEEDED,
                    output = Data.Builder()
                        .putBoolean(MediaStoreDiscoveryWorker.KEY_HAS_MORE, false)
                        .putString(
                            MediaStoreDiscoveryWorker.KEY_ACCESS_SCOPE,
                            ImageLibraryAccessScope.FULL_LIBRARY.name,
                        )
                        .build(),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            MediaStoreIndexingState.COMPLETED(1, false, ImageLibraryAccessScope.FULL_LIBRARY),
            viewModel.uiState.value.indexing,
        )
    }

    @Test
    fun representsSelectedPhotoAccessWithoutCallingItFullLibraryAccess() = runTest {
        val scheduler = RecordingScheduler()
        val viewModel = viewModel(
            discovery = scheduler,
            assets = RecordingAssetRepository(photoCount = 2),
        )

        viewModel.onPhotoPermissionResult(isGranted = true)
        viewModel.onIndexRequested()
        scheduler.emit(
            listOf(
                workInfo(
                    state = WorkInfo.State.SUCCEEDED,
                    output = Data.Builder()
                        .putBoolean(MediaStoreDiscoveryWorker.KEY_HAS_MORE, true)
                        .putString(
                            MediaStoreDiscoveryWorker.KEY_ACCESS_SCOPE,
                            ImageLibraryAccessScope.SELECTED_PHOTOS.name,
                        )
                        .build(),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            MediaStoreIndexingState.COMPLETED(2, true, ImageLibraryAccessScope.SELECTED_PHOTOS),
            viewModel.uiState.value.indexing,
        )
    }

    @Test
    fun mapsLostAccessAndFailuresToExplicitRecoveryStates() = runTest {
        val accessScheduler = RecordingScheduler()
        val accessVm = viewModel(discovery = accessScheduler)
        accessVm.onPhotoPermissionResult(isGranted = true)
        accessVm.onIndexRequested()
        accessScheduler.emit(
            listOf(
                workInfo(
                    state = WorkInfo.State.FAILED,
                    output = Data.Builder()
                        .putString(
                            MediaStoreDiscoveryWorker.KEY_FAILURE_REASON,
                            MediaStoreDiscoveryWorker.REASON_ACCESS_STOPPED,
                        )
                        .build(),
                ),
            ),
        )
        advanceUntilIdle()
        assertEquals(
            MediaStoreSetupUiState(
                photoAccess = PhotoAccessState.REQUIRED,
                indexing = MediaStoreIndexingState.ACCESS_REVOKED,
            ),
            accessVm.uiState.value,
        )

        val failScheduler = RecordingScheduler()
        val failVm = viewModel(discovery = failScheduler)
        failVm.onPhotoPermissionResult(isGranted = true)
        failVm.onIndexRequested()
        failScheduler.emit(
            listOf(
                workInfo(
                    state = WorkInfo.State.FAILED,
                    output = Data.Builder()
                        .putString(MediaStoreDiscoveryWorker.KEY_FAILURE_REASON, "other")
                        .build(),
                ),
            ),
        )
        advanceUntilIdle()
        assertEquals(
            MediaStoreSetupUiState(
                photoAccess = PhotoAccessState.GRANTED,
                indexing = MediaStoreIndexingState.FAILED(
                    "Memora could not finish reading photo metadata. You can try again.",
                ),
            ),
            failVm.uiState.value,
        )
    }

    @Test
    fun ignoresRepeatedIndexRequestsWhileOneRequestIsInProgress() = runTest {
        val scheduler = RecordingScheduler()
        val viewModel = viewModel(discovery = scheduler)
        viewModel.onPhotoPermissionResult(isGranted = true)

        viewModel.onIndexRequested()
        viewModel.onIndexRequested()

        assertEquals(1, scheduler.drainCount)
    }

    private fun viewModel(
        discovery: RecordingScheduler = RecordingScheduler(),
        exif: RecordingExifScheduler = RecordingExifScheduler(),
        ocr: RecordingOcrScheduler = RecordingOcrScheduler(),
        assets: RecordingAssetRepository = RecordingAssetRepository(),
        persistence: RecordingExifPersistence = RecordingExifPersistence(),
        ocrPersistence: RecordingOcrPersistence = RecordingOcrPersistence(),
    ) = MediaStoreSetupViewModel(
        discoveryWorkScheduler = discovery,
        exifExtractWorkScheduler = exif,
        screenshotOcrWorkScheduler = ocr,
        assetRepository = assets,
        imageExifPersistence = persistence,
        screenshotOcrPersistence = ocrPersistence,
    )

    private fun workInfo(state: WorkInfo.State, output: Data): WorkInfo =
        WorkInfo(
            UUID.randomUUID(),
            state,
            emptySet(),
            output,
            Data.EMPTY,
            1,
            1,
        )

    private class RecordingScheduler : MediaStoreDiscoveryWorkScheduler {
        var drainCount: Int = 0
        private val infos = MutableStateFlow<List<WorkInfo>>(emptyList())

        override fun enqueueDrain() {
            drainCount += 1
        }

        override fun enqueueContinuation() = Unit

        override fun observeUniqueWork(): Flow<List<WorkInfo>> = infos

        fun emit(value: List<WorkInfo>) {
            infos.value = value
        }
    }

    private class RecordingExifScheduler : MediaStoreImageExifExtractWorkScheduler {
        var drainCount: Int = 0
        private val infos = MutableStateFlow<List<WorkInfo>>(emptyList())

        override fun enqueueDrain(sourceId: SourceId) {
            drainCount += 1
        }

        override fun enqueueContinuation(sourceId: SourceId, afterSourceAssetKey: String) = Unit

        override fun cancel(sourceId: SourceId) = Unit

        override fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>> = infos
    }

    private class RecordingOcrScheduler : MediaStoreScreenshotOcrExtractWorkScheduler {
        var drainCount: Int = 0
        private val infos = MutableStateFlow<List<WorkInfo>>(emptyList())

        override fun enqueueDrain(sourceId: SourceId) {
            drainCount += 1
        }

        override fun enqueueContinuation(sourceId: SourceId, afterSourceAssetKey: String) = Unit

        override fun cancel(sourceId: SourceId) = Unit

        override fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>> = infos
    }

    private class RecordingAssetRepository(
        private val photoCount: Int = 1,
        private val screenshotCount: Int = 0,
    ) : AssetRepository {
        override suspend fun save(record: AssetIndexRecord) = Unit

        override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = null

        override suspend fun findFirstBySourceAndType(
            sourceId: SourceId,
            type: AssetType,
        ): Asset? = null

        override suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int =
            when (type) {
                AssetType.PHOTO -> photoCount
                AssetType.SCREENSHOT -> screenshotCount
                else -> 0
            }

        override suspend fun findNextPdfPendingLocalReading(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null

        override suspend fun findNextImagePendingExifExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null

        override suspend fun findNextScreenshotPendingOcrExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
    }

    private class RecordingExifPersistence : ImageExifExtractionPersistence {
        override suspend fun findHeader(record: ImageExifExtractionRecord) = null

        override suspend fun insert(record: ImageExifExtractionRecord) = Unit

        override suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int = 0
    }

    private class RecordingOcrPersistence : ScreenshotOcrExtractionPersistence {
        override suspend fun findHeader(record: ScreenshotOcrExtractionRecord) = null

        override suspend fun insert(record: ScreenshotOcrExtractionRecord) = Unit

        override suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int = 0
    }
}
