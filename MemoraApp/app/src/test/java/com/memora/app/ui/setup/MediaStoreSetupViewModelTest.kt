package com.memora.app.ui.setup

import com.memora.app.application.discovery.MediaStoreImageIndexer
import com.memora.app.application.discovery.MediaStoreIndexingOutcome
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
        val indexer = RecordingIndexer(MediaStoreIndexingOutcome.Indexed(1, false, ImageLibraryAccessScope.FULL_LIBRARY))
        val viewModel = MediaStoreSetupViewModel(indexer)

        viewModel.onIndexRequested()
        assertEquals(0, indexer.invocationCount)
        assertEquals(MediaStoreSetupUiState(), viewModel.uiState.value)

        viewModel.onPhotoPermissionResult(isGranted = true)
        assertEquals(0, indexer.invocationCount)
        assertEquals(PhotoAccessState.GRANTED, viewModel.uiState.value.photoAccess)
        assertEquals(MediaStoreIndexingState.NOT_STARTED, viewModel.uiState.value.indexing)

        viewModel.onIndexRequested()
        assertEquals(MediaStoreIndexingState.IN_PROGRESS, viewModel.uiState.value.indexing)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, indexer.invocationCount)
        assertEquals(
            MediaStoreIndexingState.COMPLETED(1, false, ImageLibraryAccessScope.FULL_LIBRARY),
            viewModel.uiState.value.indexing,
        )
    }

    @Test
    fun representsSelectedPhotoAccessWithoutCallingItFullLibraryAccess() = runTest {
        val viewModel = MediaStoreSetupViewModel(
            RecordingIndexer(
                MediaStoreIndexingOutcome.Indexed(
                    discoveredAssetCount = 2,
                    hasMore = true,
                    accessScope = ImageLibraryAccessScope.SELECTED_PHOTOS,
                ),
            ),
        )

        viewModel.onPhotoPermissionResult(isGranted = true)
        viewModel.onIndexRequested()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            MediaStoreIndexingState.COMPLETED(2, true, ImageLibraryAccessScope.SELECTED_PHOTOS),
            viewModel.uiState.value.indexing,
        )
    }

    @Test
    fun mapsLostAccessAndFailuresToExplicitRecoveryStates() = runTest {
        listOf(
            MediaStoreIndexingOutcome.AccessRequired to MediaStoreSetupUiState(
                photoAccess = PhotoAccessState.REQUIRED,
                indexing = MediaStoreIndexingState.ACCESS_REQUIRED,
            ),
            MediaStoreIndexingOutcome.AccessRevoked to MediaStoreSetupUiState(
                photoAccess = PhotoAccessState.REQUIRED,
                indexing = MediaStoreIndexingState.ACCESS_REVOKED,
            ),
            MediaStoreIndexingOutcome.Failed(DiscoveryFailure("busy", "The catalogue is busy.")) to
                MediaStoreSetupUiState(
                    photoAccess = PhotoAccessState.GRANTED,
                    indexing = MediaStoreIndexingState.FAILED("The catalogue is busy."),
                ),
        ).forEach { (outcome, expected) ->
            val viewModel = MediaStoreSetupViewModel(RecordingIndexer(outcome))
            viewModel.onPhotoPermissionResult(isGranted = true)

            viewModel.onIndexRequested()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(expected, viewModel.uiState.value)
        }
    }

    @Test
    fun ignoresRepeatedIndexRequestsWhileOneRequestIsInProgress() = runTest {
        val indexer = RecordingIndexer(
            MediaStoreIndexingOutcome.Indexed(0, false, ImageLibraryAccessScope.FULL_LIBRARY),
        )
        val viewModel = MediaStoreSetupViewModel(indexer)
        viewModel.onPhotoPermissionResult(isGranted = true)

        viewModel.onIndexRequested()
        viewModel.onIndexRequested()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, indexer.invocationCount)
    }

    private class RecordingIndexer(
        private val outcome: MediaStoreIndexingOutcome,
    ) : MediaStoreImageIndexer {
        var invocationCount: Int = 0

        override suspend fun invoke(): MediaStoreIndexingOutcome {
            invocationCount += 1
            return outcome
        }
    }
}
