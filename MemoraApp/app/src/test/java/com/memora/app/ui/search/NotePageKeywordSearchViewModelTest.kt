package com.memora.app.ui.search

import com.memora.app.application.notes.NotePageKeywordSearchHit
import com.memora.app.application.notes.NotePageKeywordSearchOutcome
import com.memora.app.application.notes.NotePageKeywordSearchReadiness
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

    private fun viewModel(
        readiness: suspend () -> NotePageKeywordSearchReadiness = {
            NotePageKeywordSearchReadiness(noteCount = 2)
        },
        minSearchingVisibleMs: Long = 0L,
        search: suspend (String) -> NotePageKeywordSearchOutcome,
    ) = NotePageKeywordSearchViewModel(
        searchPersistedNotePageText = search,
        loadReadiness = readiness,
        minSearchingVisibleMs = minSearchingVisibleMs,
        monotonicMs = { 0L },
    )

    private fun sampleHit() = NotePageKeywordSearchHit(
        label = "Ideas",
        excerpt = "…travel plan…",
        sourceId = "microsoft.onenote",
        sourceAssetKey = "p1",
    )
}
