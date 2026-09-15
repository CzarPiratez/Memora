package com.memora.app.ui.search

import com.memora.app.application.memory.CanonicalRecallTestFixtures
import com.memora.app.application.notes.NotePageKeywordSearchHit
import com.memora.app.domain.asset.AssetType
import com.memora.app.application.notes.NotePageKeywordSearchOutcome
import com.memora.app.application.notes.NotePageKeywordSearchReadiness
import com.memora.app.application.notes.OpenPersistedNotePageResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotePageKeywordSearchViewModelTest {
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
            NotePageKeywordSearchOutcome.Matches(
                query = "plan",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
        advanceUntilIdle()

        viewModel.onQueryChanged("plan")
        viewModel.onSearch()
        testScheduler.runCurrent()
        assertEquals(NotePageKeywordSearchPhase.Searching, viewModel.uiState.value.phase)

        viewModel.onSearchCancelled()
        assertEquals(NotePageKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("plan", viewModel.uiState.value.query)

        viewModel.onSearch()
        advanceUntilIdle()
        val results = viewModel.uiState.value.phase as NotePageKeywordSearchPhase.Results
        assertEquals("plan", results.query)
        assertEquals(1, results.hits.size)
        assertTrue(viewModel.uiState.value.canClearQuery)
    }

    @Test
    fun blank_query_and_nothing_saved_are_distinct() = runTest {
        val viewModel = viewModel(
            readiness = { NotePageKeywordSearchReadiness(noteCount = 0) },
        ) { raw ->
            if (raw.isBlank()) {
                NotePageKeywordSearchOutcome.BlankQuery
            } else {
                NotePageKeywordSearchOutcome.NothingSavedToSearch(query = raw.trim())
            }
        }
        advanceUntilIdle()

        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(NotePageKeywordSearchPhase.EmptyQuery, viewModel.uiState.value.phase)

        viewModel.onQueryChanged("hello")
        viewModel.onSearch()
        advanceUntilIdle()
        assertTrue(
            viewModel.uiState.value.phase is NotePageKeywordSearchPhase.NothingSavedToSearch,
        )
    }

    @Test
    fun open_original_launches_ready_url_and_clears_feedback() = runTest {
        val launched = mutableListOf<Pair<String?, String?>>()
        val viewModel = viewModel(
            open = { _, _ ->
                OpenPersistedNotePageResult.Ready(
                    webUrl = "https://onenote.example/web",
                    clientUrl = "onenote:https://onenote.example/client",
                )
            },
            launch = { web, client ->
                launched += web to client
                true
            },
        ) {
            NotePageKeywordSearchOutcome.Matches(
                query = "plan",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
        advanceUntilIdle()
        viewModel.onQueryChanged("plan")
        viewModel.onSearch()
        advanceUntilIdle()

        viewModel.onOpenOriginalNote(sampleHit())
        advanceUntilIdle()
        assertEquals(
            listOf("https://onenote.example/web" to "onenote:https://onenote.example/client"),
            launched,
        )
        assertEquals(NotePageOpenFeedbackUi.None, viewModel.uiState.value.openFeedback)
    }

    @Test
    fun open_original_maps_source_unavailable() = runTest {
        val viewModel = viewModel(
            open = { _, _ -> OpenPersistedNotePageResult.SourceUnavailable },
        ) {
            NotePageKeywordSearchOutcome.Matches(
                query = "plan",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
        advanceUntilIdle()
        viewModel.onQueryChanged("plan")
        viewModel.onSearch()
        advanceUntilIdle()

        viewModel.onOpenOriginalNote(sampleHit())
        advanceUntilIdle()
        assertEquals(
            NotePageOpenFeedbackUi.SourceUnavailable(target("p1")),
            viewModel.uiState.value.openFeedback,
        )
        // The failure belongs to the page that failed. Every other card is
        // untouched, so the list does not read as if all of them were tapped.
        assertEquals(FindCardOpenState.SOURCE_UNAVAILABLE, viewModel.uiState.value.openStateFor(target("p1")))
        assertEquals(FindCardOpenState.IDLE, viewModel.uiState.value.openStateFor(target("p2")))
    }

    /**
     * The reported defect: one screen-level flag meant tapping one page greyed
     * out every Open button, so the list looked like it had all been tapped
     * while a OneNote open waited on Graph.
     */
    @Test
    fun opening_belongs_to_the_tapped_page_and_leaves_the_other_cards_live() = runTest {
        val graph = CompletableDeferred<OpenPersistedNotePageResult>()
        val viewModel = viewModel(open = { _, _ -> graph.await() }) { searchMatches() }
        advanceUntilIdle()
        viewModel.onQueryChanged("plan")
        viewModel.onSearch()
        advanceUntilIdle()

        viewModel.onOpenOriginalNote(sampleHit("p1"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(NotePageOpenFeedbackUi.Opening(target("p1")), state.openFeedback)
        assertEquals(FindCardOpenState.OPENING, state.openStateFor(target("p1")))
        assertEquals(FindCardOpenState.IDLE, state.openStateFor(target("p2")))

        graph.complete(OpenPersistedNotePageResult.CouldNotOpen)
        advanceUntilIdle()
    }

    /**
     * Ignoring the second tap while Graph is still answering reads as a dead
     * button for the seconds that call takes.
     */
    @Test
    fun a_second_tap_supersedes_the_open_in_flight() = runTest {
        val firstGraph = CompletableDeferred<OpenPersistedNotePageResult>()
        val opened = mutableListOf<String>()
        val viewModel = viewModel(
            open = { _, key ->
                opened += key
                if (key == "p1") firstGraph.await() else OpenPersistedNotePageResult.CouldNotOpen
            },
        ) { searchMatches() }
        advanceUntilIdle()
        viewModel.onQueryChanged("plan")
        viewModel.onSearch()
        advanceUntilIdle()

        viewModel.onOpenOriginalNote(sampleHit("p1"))
        advanceUntilIdle()
        viewModel.onOpenOriginalNote(sampleHit("p2"))
        advanceUntilIdle()

        assertEquals(listOf("p1", "p2"), opened)
        assertEquals(
            NotePageOpenFeedbackUi.CouldNotOpen(target("p2")),
            viewModel.uiState.value.openFeedback,
        )

        // The abandoned call must not repaint the card it no longer owns.
        firstGraph.complete(OpenPersistedNotePageResult.SourceUnavailable)
        advanceUntilIdle()
        assertEquals(
            NotePageOpenFeedbackUi.CouldNotOpen(target("p2")),
            viewModel.uiState.value.openFeedback,
        )
    }

    /** A Graph round trip is no reason to freeze the search box. */
    @Test
    fun an_open_in_flight_does_not_lock_search() = runTest {
        val graph = CompletableDeferred<OpenPersistedNotePageResult>()
        val viewModel = viewModel(open = { _, _ -> graph.await() }) { searchMatches() }
        advanceUntilIdle()
        viewModel.onQueryChanged("plan")
        viewModel.onSearch()
        advanceUntilIdle()

        viewModel.onOpenOriginalNote(sampleHit("p1"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canSubmitSearch)
        assertTrue(viewModel.uiState.value.canClearQuery)

        viewModel.onSearch()
        advanceUntilIdle()
        // The list the open belonged to has been replaced, so the feedback goes
        // with it rather than pointing at a card that is no longer there.
        assertEquals(NotePageOpenFeedbackUi.None, viewModel.uiState.value.openFeedback)

        graph.complete(OpenPersistedNotePageResult.SourceUnavailable)
        advanceUntilIdle()
        assertEquals(NotePageOpenFeedbackUi.None, viewModel.uiState.value.openFeedback)
    }

    private fun searchMatches() = NotePageKeywordSearchOutcome.Matches(
        query = "plan",
        hits = listOf(sampleHit("p1"), sampleHit("p2")),
        limitReached = false,
    )

    private fun target(sourceAssetKey: String) = FindOpenTarget(
        sourceId = "microsoft.onenote",
        sourceAssetKey = sourceAssetKey,
    )

    private fun viewModel(
        readiness: suspend () -> NotePageKeywordSearchReadiness = {
            NotePageKeywordSearchReadiness(noteCount = 2)
        },
        minSearchingVisibleMs: Long = 0L,
        open: suspend (String, String) -> OpenPersistedNotePageResult = { _, _ ->
            OpenPersistedNotePageResult.CouldNotOpen
        },
        launch: (String?, String?) -> Boolean = { _, _ -> true },
        search: suspend (String) -> NotePageKeywordSearchOutcome,
    ) = NotePageKeywordSearchViewModel(
        searchPersistedNotePageText = search,
        loadReadiness = readiness,
        openPersistedNotePage = open,
        launchOneNoteOriginal = launch,
        minSearchingVisibleMs = minSearchingVisibleMs,
        monotonicMs = { 0L },
    )

    private fun sampleHit(sourceAssetKey: String = "p1") = NotePageKeywordSearchHit(
        recall = CanonicalRecallTestFixtures.keywordRecall(
            assetType = AssetType.NOTE,
            label = "Ideas",
            excerpt = "…travel plan…",
            pageNumber = null,
            sourceId = "microsoft.onenote",
            sourceAssetKey = sourceAssetKey,
        ),
    )
}
