package com.memora.app.ui.search

import com.memora.app.application.images.ScreenshotOcrKeywordSearchHit
import com.memora.app.application.images.ScreenshotOcrKeywordSearchOutcome
import com.memora.app.application.images.ScreenshotOcrKeywordSearchReadiness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotOcrKeywordSearchViewModelTest {
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
    fun search_maps_matches_and_keeps_query_on_cancel() = runTest {
        val viewModel = ScreenshotOcrKeywordSearchViewModel(
            searchPersistedScreenshotOcrText = {
                ScreenshotOcrKeywordSearchOutcome.Matches(
                    query = "note",
                    hits = listOf(
                        ScreenshotOcrKeywordSearchHit(
                            label = "Screenshot_memora_note.png",
                            excerpt = "Screenshot note",
                            sourceId = "android-media-store-images",
                            sourceAssetKey = "external_primary:1",
                        ),
                    ),
                    limitReached = false,
                )
            },
            loadReadiness = { ScreenshotOcrKeywordSearchReadiness(screenshotCount = 1) },
            minSearchingVisibleMs = 5_000L,
            monotonicMs = { 0L },
        )
        advanceUntilIdle()

        viewModel.onQueryChanged("note")
        viewModel.onSearch()
        testScheduler.runCurrent()
        assertEquals(ScreenshotOcrKeywordSearchPhase.Searching, viewModel.uiState.value.phase)

        viewModel.onSearchCancelled()
        assertEquals(ScreenshotOcrKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("note", viewModel.uiState.value.query)

        viewModel.onSearch()
        advanceUntilIdle()
        val results = viewModel.uiState.value.phase as ScreenshotOcrKeywordSearchPhase.Results
        assertEquals("note", results.query)
        assertEquals(1, results.hits.size)
        assertTrue(viewModel.uiState.value.canClearQuery)
    }

    @Test
    fun blank_query_and_nothing_saved_are_distinct() = runTest {
        val viewModel = ScreenshotOcrKeywordSearchViewModel(
            searchPersistedScreenshotOcrText = { raw ->
                if (raw.isBlank()) {
                    ScreenshotOcrKeywordSearchOutcome.BlankQuery
                } else {
                    ScreenshotOcrKeywordSearchOutcome.NothingSavedToSearch(query = raw.trim())
                }
            },
            loadReadiness = { ScreenshotOcrKeywordSearchReadiness(screenshotCount = 0) },
            minSearchingVisibleMs = 0L,
            monotonicMs = { 0L },
        )
        advanceUntilIdle()

        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(ScreenshotOcrKeywordSearchPhase.EmptyQuery, viewModel.uiState.value.phase)

        viewModel.onQueryChanged("hello")
        viewModel.onSearch()
        advanceUntilIdle()
        assertTrue(
            viewModel.uiState.value.phase is ScreenshotOcrKeywordSearchPhase.NothingSavedToSearch,
        )
    }
}
