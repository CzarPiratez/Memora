package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningOpenOriginalResult
import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.application.intelligence.MeaningSearchReadiness
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.CorpusCompletenessCounts
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeaningSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val model = ModelVersionIdentity("m", "1")

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
            MeaningSearchOutcome.Matches(
                query = "cafe",
                hits = listOf(sampleHit()),
                limitReached = false,
                model = model,
            )
        }
        advanceUntilIdle()

        viewModel.onQueryChanged("cafe")
        viewModel.onSearch()
        testScheduler.runCurrent()
        assertEquals(MeaningSearchPhase.Searching, viewModel.uiState.value.phase)

        viewModel.onSearchCancelled()
        assertEquals(MeaningSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("cafe", viewModel.uiState.value.query)

        viewModel.onSearch()
        advanceUntilIdle()
        val results = viewModel.uiState.value.phase as MeaningSearchPhase.Results
        assertEquals("cafe", results.query)
        assertEquals(1, results.hits.size)
        assertTrue(viewModel.uiState.value.canClearQuery)
    }

    @Test
    fun blank_query_and_nothing_indexed_are_distinct() = runTest {
        val viewModel = viewModel { raw ->
            if (raw.isBlank()) {
                MeaningSearchOutcome.BlankQuery
            } else {
                MeaningSearchOutcome.NothingIndexed(query = raw.trim())
            }
        }
        advanceUntilIdle()

        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(MeaningSearchPhase.EmptyQuery, viewModel.uiState.value.phase)

        viewModel.onQueryChanged("hello")
        viewModel.onSearch()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.phase is MeaningSearchPhase.NothingIndexed)
    }

    @Test
    fun open_original_screenshot_sets_preview() = runTest {
        val pixels = IntArray(4) { 0xFF0000FF.toInt() }
        val viewModel = viewModel(
            open = { _, _ ->
                MeaningOpenOriginalResult.ScreenshotReady(
                    label = "Screenshot_memora_note.png",
                    widthPx = 2,
                    heightPx = 2,
                    argb8888 = pixels,
                )
            },
        ) {
            MeaningSearchOutcome.Matches(
                query = "note",
                hits = listOf(sampleHit()),
                limitReached = false,
                model = model,
            )
        }
        advanceUntilIdle()
        viewModel.onQueryChanged("note")
        viewModel.onSearch()
        advanceUntilIdle()

        viewModel.onOpenOriginal(sampleHit())
        advanceUntilIdle()
        val preview = viewModel.uiState.value.originalPreview
        assertTrue(preview is MeaningOriginalPreviewUi.Screenshot)
        assertEquals(
            "Screenshot_memora_note.png",
            (preview as MeaningOriginalPreviewUi.Screenshot).preview.screenshotLabel,
        )
        assertEquals(MeaningOpenFeedbackUi.None, viewModel.uiState.value.openFeedback)
    }

    @Test
    fun open_original_note_launches_urls() = runTest {
        val launched = mutableListOf<Pair<String?, String?>>()
        val viewModel = viewModel(
            open = { _, _ ->
                MeaningOpenOriginalResult.NoteReady(
                    webUrl = "https://onenote.example/web",
                    clientUrl = "onenote:https://onenote.example/client",
                )
            },
            launch = { web, client ->
                launched += web to client
                true
            },
        ) {
            MeaningSearchOutcome.Matches(
                query = "note",
                hits = listOf(sampleHit(AssetType.NOTE)),
                limitReached = false,
                model = model,
            )
        }
        advanceUntilIdle()
        viewModel.onOpenOriginal(sampleHit(AssetType.NOTE))
        advanceUntilIdle()
        assertEquals(1, launched.size)
        assertNotNull(launched.first().first)
        assertEquals(MeaningOpenFeedbackUi.None, viewModel.uiState.value.openFeedback)
    }

    private fun viewModel(
        readiness: suspend () -> MeaningSearchReadiness = {
            MeaningSearchReadiness.Ready(
                model = model,
                indexedCount = 1,
                memoriesReadyCount = 1,
                corpusCompleteness = CorpusCompletenessSnapshot(
                    counts = CorpusCompletenessCounts(
                        memoriesReady = 1,
                        memoriesPendingAssembly = 0,
                        meaningSummaryIndexed = 1,
                        meaningEvidenceIndexed = 0,
                        meaningIndexPending = 0,
                    ),
                    blocked = null,
                ),
            )
        },
        open: suspend (MeaningSearchHit, String) -> MeaningOpenOriginalResult = { _, _ ->
            MeaningOpenOriginalResult.CouldNotOpen
        },
        launch: (String?, String?) -> Boolean = { _, _ -> false },
        minSearchingVisibleMs: Long = 0L,
        search: suspend (String) -> MeaningSearchOutcome,
    ) = MeaningSearchViewModel(
        searchByMeaning = search,
        loadReadiness = readiness,
        openOriginal = open,
        launchOneNoteOriginal = launch,
        minSearchingVisibleMs = minSearchingVisibleMs,
    ).also { it.onScreenVisible() }

    private fun sampleHit(type: AssetType = AssetType.SCREENSHOT) = MeaningSearchHit(
        revisionId = MemoryRevisionId("r1"),
        memoryId = MemoryId("m1"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey("k"),
        assetType = type,
        label = "Screenshot_memora_note.png",
        summaryText = "Screenshot note",
        score = 0.7f,
        model = model,
    )
}
