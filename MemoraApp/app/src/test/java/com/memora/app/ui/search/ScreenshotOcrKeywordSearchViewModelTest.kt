package com.memora.app.ui.search

import com.memora.app.application.images.ScreenshotOcrKeywordSearchHit
import com.memora.app.application.images.ScreenshotOcrKeywordSearchOutcome
import com.memora.app.application.images.ScreenshotOcrKeywordSearchReadiness
import com.memora.app.application.images.ScreenshotPreviewRenderResult
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
        val viewModel = viewModel(minSearchingVisibleMs = 5_000L) {
            ScreenshotOcrKeywordSearchOutcome.Matches(
                query = "note",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
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
        val viewModel = viewModel(
            readiness = { ScreenshotOcrKeywordSearchReadiness(screenshotCount = 0) },
        ) { raw ->
            if (raw.isBlank()) {
                ScreenshotOcrKeywordSearchOutcome.BlankQuery
            } else {
                ScreenshotOcrKeywordSearchOutcome.NothingSavedToSearch(query = raw.trim())
            }
        }
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

    @Test
    fun open_original_shows_preview_for_ready_render() = runTest {
        val pixels = intArrayOf(
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt(),
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt(),
        )
        val viewModel = viewModel(
            open = {
                ScreenshotPreviewRenderResult.Ready(
                    screenshotLabel = it.label,
                    widthPx = 2,
                    heightPx = 2,
                    argb8888 = pixels,
                )
            },
        ) {
            ScreenshotOcrKeywordSearchOutcome.Matches(
                query = "note",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }

        viewModel.onQueryChanged("note")
        viewModel.onSearch()
        advanceUntilIdle()
        val hit = (viewModel.uiState.value.phase as ScreenshotOcrKeywordSearchPhase.Results)
            .hits.first()
        viewModel.onOpenOriginalScreenshot(hit)
        advanceUntilIdle()

        val preview = viewModel.uiState.value.originalPreview
        assertEquals("Screenshot_memora_note.png", preview?.screenshotLabel)
        assertEquals(2, preview?.widthPx)
        assertEquals(ScreenshotOpenFeedbackUi.None, viewModel.uiState.value.openFeedback)
    }

    @Test
    fun open_original_maps_source_unavailable_to_feedback() = runTest {
        val viewModel = viewModel(
            open = { ScreenshotPreviewRenderResult.SourceUnavailable },
        ) {
            ScreenshotOcrKeywordSearchOutcome.Matches(
                query = "note",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
        viewModel.onQueryChanged("note")
        viewModel.onSearch()
        advanceUntilIdle()
        viewModel.onOpenOriginalScreenshot(sampleHit())
        advanceUntilIdle()
        assertEquals(
            ScreenshotOpenFeedbackUi.SourceUnavailable,
            viewModel.uiState.value.openFeedback,
        )
        assertEquals(null, viewModel.uiState.value.originalPreview)
    }

    private fun viewModel(
        readiness: suspend () -> ScreenshotOcrKeywordSearchReadiness = {
            ScreenshotOcrKeywordSearchReadiness(screenshotCount = 1)
        },
        open: suspend (ScreenshotOcrKeywordSearchHit) -> ScreenshotPreviewRenderResult = {
            ScreenshotPreviewRenderResult.CouldNotOpen
        },
        minSearchingVisibleMs: Long = 0L,
        monotonicMs: () -> Long = { 0L },
        search: suspend (String) -> ScreenshotOcrKeywordSearchOutcome,
    ) = ScreenshotOcrKeywordSearchViewModel(
        searchScreenshotKeyword = search,
        loadReadiness = readiness,
        openPersistedScreenshotForViewing = open,
        minSearchingVisibleMs = minSearchingVisibleMs,
        monotonicMs = monotonicMs,
    )

    private fun sampleHit() = ScreenshotOcrKeywordSearchHit(
        label = "Screenshot_memora_note.png",
        excerpt = "Screenshot note",
        sourceId = "android-media-store-images",
        sourceAssetKey = "external_primary:1",
    )
}
