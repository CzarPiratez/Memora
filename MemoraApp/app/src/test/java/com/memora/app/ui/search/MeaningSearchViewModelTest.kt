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
import kotlinx.coroutines.CompletableDeferred
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
        assertEquals("s", preview.preview.sourceId)
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

    @Test
    fun search_failure_leaves_recoverable_phase_not_spinning() = runTest {
        val viewModel = viewModel { error("simulated search failure") }
        viewModel.onQueryChanged("invoice")
        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(MeaningSearchPhase.SearchCouldNotFinish, viewModel.uiState.value.phase)
    }

    @Test
    fun failed_outcome_maps_to_search_could_not_finish() = runTest {
        val viewModel = viewModel {
            MeaningSearchOutcome.Failed(reason = "On-device embedding failed.")
        }
        viewModel.onQueryChanged("invoice")
        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(MeaningSearchPhase.SearchCouldNotFinish, viewModel.uiState.value.phase)
    }

    @Test
    fun engine_unavailable_maps_to_distinct_phase() = runTest {
        val viewModel = viewModel {
            MeaningSearchOutcome.EngineUnavailable(reason = "model missing")
        }
        viewModel.onQueryChanged("invoice")
        viewModel.onSearch()
        advanceUntilIdle()
        val phase = viewModel.uiState.value.phase as MeaningSearchPhase.EngineUnavailable
        assertEquals("model missing", phase.reason)
    }

    @Test
    fun readiness_load_failure_maps_to_could_not_load() = runTest {
        val viewModel = MeaningSearchViewModel(
            searchByMeaning = { MeaningSearchOutcome.NothingIndexed("x") },
            loadReadiness = { error("readiness failed") },
            openOriginal = { _, _ -> MeaningOpenOriginalResult.CouldNotOpen },
            launchOneNoteOriginal = { _, _ -> false },
            minSearchingVisibleMs = 0L,
        )
        viewModel.onScreenVisible()
        advanceUntilIdle()
        assertEquals(MeaningSearchReadinessUi.CouldNotLoad, viewModel.uiState.value.readiness)
    }

    /**
     * The reported defect: one screen-level flag meant tapping one result greyed
     * out every Open button, so the list looked like it had all been tapped
     * while a OneNote open waited on Graph.
     */
    @Test
    fun opening_belongs_to_the_tapped_card_and_leaves_the_others_live() = runTest {
        val graph = CompletableDeferred<MeaningOpenOriginalResult>()
        val viewModel = viewModel(open = { _, _ -> graph.await() }) { noteMatches() }
        advanceUntilIdle()
        viewModel.onQueryChanged("note")
        viewModel.onSearch()
        advanceUntilIdle()

        viewModel.onOpenOriginal(sampleHit(AssetType.NOTE, key = "k1"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(MeaningOpenFeedbackUi.Opening(target("k1")), state.openFeedback)
        assertEquals(FindCardOpenState.OPENING, state.openStateFor(target("k1")))
        assertEquals(FindCardOpenState.IDLE, state.openStateFor(target("k2")))
        // A Graph round trip is no reason to freeze the search box.
        assertTrue(state.canSubmitSearch)

        graph.complete(MeaningOpenOriginalResult.CouldNotOpen)
        advanceUntilIdle()
        assertEquals(
            MeaningOpenFeedbackUi.CouldNotOpen(target("k1")),
            viewModel.uiState.value.openFeedback,
        )
        assertEquals(
            FindCardOpenState.IDLE,
            viewModel.uiState.value.openStateFor(target("k2")),
        )
    }

    /**
     * Ignoring the second tap while Graph is still answering reads as a dead
     * button for the seconds that call takes.
     */
    @Test
    fun a_second_tap_supersedes_the_open_in_flight() = runTest {
        val firstGraph = CompletableDeferred<MeaningOpenOriginalResult>()
        val opened = mutableListOf<String>()
        val viewModel = viewModel(
            open = { hit, _ ->
                opened += hit.sourceAssetKey.value
                if (hit.sourceAssetKey.value == "k1") {
                    firstGraph.await()
                } else {
                    MeaningOpenOriginalResult.SourceUnavailable
                }
            },
        ) { noteMatches() }
        advanceUntilIdle()
        viewModel.onQueryChanged("note")
        viewModel.onSearch()
        advanceUntilIdle()

        viewModel.onOpenOriginal(sampleHit(AssetType.NOTE, key = "k1"))
        advanceUntilIdle()
        viewModel.onOpenOriginal(sampleHit(AssetType.NOTE, key = "k2"))
        advanceUntilIdle()

        assertEquals(listOf("k1", "k2"), opened)
        assertEquals(
            MeaningOpenFeedbackUi.SourceUnavailable(target("k2")),
            viewModel.uiState.value.openFeedback,
        )

        // The abandoned call must not repaint the card it no longer owns.
        firstGraph.complete(MeaningOpenOriginalResult.CouldNotOpen)
        advanceUntilIdle()
        assertEquals(
            MeaningOpenFeedbackUi.SourceUnavailable(target("k2")),
            viewModel.uiState.value.openFeedback,
        )
    }

    private fun noteMatches() = MeaningSearchOutcome.Matches(
        query = "note",
        hits = listOf(
            sampleHit(AssetType.NOTE, key = "k1"),
            sampleHit(AssetType.NOTE, key = "k2"),
        ),
        limitReached = false,
        model = model,
    )

    private fun target(sourceAssetKey: String) = FindOpenTarget(
        sourceId = "s",
        sourceAssetKey = sourceAssetKey,
    )

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

    private fun sampleHit(
        type: AssetType = AssetType.SCREENSHOT,
        key: String = "k",
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId("r1"),
        memoryId = MemoryId("m1"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey(key),
        assetType = type,
        label = "Screenshot_memora_note.png",
        summaryText = "Screenshot note",
        score = 0.7f,
        model = model,
    )
}
