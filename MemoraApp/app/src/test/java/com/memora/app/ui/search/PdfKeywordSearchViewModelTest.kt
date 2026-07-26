package com.memora.app.ui.search

import com.memora.app.application.documents.PdfKeywordSearchHit
import com.memora.app.application.documents.PdfKeywordSearchOutcome
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PdfKeywordSearchViewModelTest {
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
    fun editing_query_clears_stale_results_so_why_cannot_cite_a_prior_search() = runTest {
        val viewModel = PdfKeywordSearchViewModel { raw ->
            PdfKeywordSearchOutcome.Matches(
                query = raw.trim(),
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }

        viewModel.onQueryChanged("meet mira")
        viewModel.onSearch()
        advanceUntilIdle()

        val results = viewModel.uiState.value.phase as PdfKeywordSearchPhase.Results
        assertEquals("meet mira", results.query)

        viewModel.onQueryChanged("meet")
        assertEquals("meet", viewModel.uiState.value.query)
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
    }

    @Test
    fun results_phase_keeps_submitted_query_for_explain_mode() = runTest {
        val viewModel = PdfKeywordSearchViewModel {
            PdfKeywordSearchOutcome.Matches(
                query = "meet mira",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }

        viewModel.onQueryChanged("meet mira")
        viewModel.onSearch()
        advanceUntilIdle()

        val phase = viewModel.uiState.value.phase as PdfKeywordSearchPhase.Results
        assertEquals("meet mira", phase.query)
        assertEquals(1, phase.hits.size)
    }

    @Test
    fun superseded_in_flight_search_does_not_overwrite_a_newer_edit() = runTest {
        val firstSearch = CompletableDeferred<PdfKeywordSearchOutcome>()
        var callCount = 0
        val viewModel = PdfKeywordSearchViewModel {
            callCount += 1
            if (callCount == 1) {
                firstSearch.await()
            } else {
                PdfKeywordSearchOutcome.Matches(
                    query = "second",
                    hits = listOf(sampleHit(label = "second.pdf")),
                    limitReached = false,
                )
            }
        }

        viewModel.onQueryChanged("first")
        viewModel.onSearch()
        dispatcher.scheduler.runCurrent()
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)

        viewModel.onQueryChanged("second")
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)

        viewModel.onSearch()
        advanceUntilIdle()
        firstSearch.complete(
            PdfKeywordSearchOutcome.Matches(
                query = "first",
                hits = listOf(sampleHit(label = "first.pdf")),
                limitReached = false,
            ),
        )
        advanceUntilIdle()

        val phase = viewModel.uiState.value.phase as PdfKeywordSearchPhase.Results
        assertEquals("second", phase.query)
        assertEquals("second.pdf", phase.hits.single().label)
    }

    @Test
    fun blank_query_maps_to_empty_query_phase() = runTest {
        val viewModel = PdfKeywordSearchViewModel {
            PdfKeywordSearchOutcome.BlankQuery
        }
        viewModel.onQueryChanged("   ")
        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(PdfKeywordSearchPhase.EmptyQuery, viewModel.uiState.value.phase)
    }

    @Test
    fun empty_hits_map_to_no_matches_with_submitted_query() = runTest {
        val viewModel = PdfKeywordSearchViewModel {
            PdfKeywordSearchOutcome.Matches(
                query = "zzz",
                hits = emptyList(),
                limitReached = false,
            )
        }
        viewModel.onQueryChanged("zzz")
        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(PdfKeywordSearchPhase.NoMatches(query = "zzz"), viewModel.uiState.value.phase)
    }

    @Test
    fun nothing_saved_outcome_maps_to_distinct_phase() = runTest {
        val viewModel = PdfKeywordSearchViewModel {
            PdfKeywordSearchOutcome.NothingSavedToSearch(query = "meet mira")
        }
        viewModel.onQueryChanged("meet mira")
        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(
            PdfKeywordSearchPhase.NothingSavedToSearch(query = "meet mira"),
            viewModel.uiState.value.phase,
        )
    }

    @Test
    fun search_failure_leaves_recoverable_phase_not_spinning() = runTest {
        val viewModel = PdfKeywordSearchViewModel {
            error("simulated search failure")
        }
        viewModel.onQueryChanged("meet")
        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(
            PdfKeywordSearchPhase.SearchCouldNotFinish,
            viewModel.uiState.value.phase,
        )
    }

    private fun sampleHit(label: String = "fixture.pdf") = PdfKeywordSearchHit(
        label = label,
        pageNumber = 1,
        excerpt = "meet mira excerpt",
        sourceId = "source-1",
        sourceAssetKey = "asset-1",
    )
}
