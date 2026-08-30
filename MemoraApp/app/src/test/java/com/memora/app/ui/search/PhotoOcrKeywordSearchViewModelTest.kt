package com.memora.app.ui.search

import com.memora.app.application.images.PhotoOcrKeywordSearchHit
import com.memora.app.application.images.PhotoOcrKeywordSearchOutcome
import com.memora.app.application.images.PhotoOcrKeywordSearchReadiness
import com.memora.app.application.images.PhotoPreviewRenderResult
import com.memora.app.application.memory.CanonicalRecallTestFixtures
import com.memora.app.domain.asset.AssetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PhotoOcrKeywordSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun searchAndOpenPreviewUsePhotoPath() = runTest {
        val hit = PhotoOcrKeywordSearchHit(
            recall = CanonicalRecallTestFixtures.keywordRecall(
                assetType = AssetType.PHOTO,
                label = "receipt.jpg",
                excerpt = "Total 42",
                pageNumber = null,
                sourceId = "media",
                sourceAssetKey = "photo-1",
            ),
        )
        val viewModel = PhotoOcrKeywordSearchViewModel(
            search = { PhotoOcrKeywordSearchOutcome.Matches("total", listOf(hit), false) },
            loadReadiness = { PhotoOcrKeywordSearchReadiness(1) },
            openPhoto = {
                PhotoPreviewRenderResult.Ready(it.label, 1, 1, intArrayOf(0))
            },
        )
        advanceUntilIdle()
        viewModel.onQueryChanged("total")
        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(1, (viewModel.uiState.value.phase as PhotoOcrKeywordSearchPhase.Results).hits.size)

        viewModel.onOpenOriginalPhoto(hit)
        advanceUntilIdle()
        assertEquals("receipt.jpg", viewModel.uiState.value.originalPreview?.photoLabel)
    }

    @Test
    fun cancelAndClearInvalidateResults() = runTest {
        val viewModel = PhotoOcrKeywordSearchViewModel(
            search = { PhotoOcrKeywordSearchOutcome.Matches(it, emptyList(), false) },
            loadReadiness = { PhotoOcrKeywordSearchReadiness(0) },
            openPhoto = { PhotoPreviewRenderResult.CouldNotOpen },
        )
        advanceUntilIdle()
        viewModel.onQueryChanged("text")
        viewModel.onSearch()
        dispatcher.scheduler.runCurrent()
        viewModel.onSearchCancelled()
        assertEquals(PhotoOcrKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        viewModel.onDerivedDataCleared()
        assertEquals("", viewModel.uiState.value.query)
    }
}
