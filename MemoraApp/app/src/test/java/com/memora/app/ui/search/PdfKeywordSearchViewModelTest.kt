package com.memora.app.ui.search

import com.memora.app.application.documents.PdfKeywordSearchHit
import com.memora.app.application.documents.PdfKeywordSearchOutcome
import com.memora.app.application.documents.PdfKeywordSearchReadiness
import com.memora.app.application.documents.PdfPagePreviewRenderResult
import com.memora.app.application.memory.CanonicalRecallTestFixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        val viewModel = viewModel { raw ->
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
        val viewModel = viewModel {
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
        val viewModel = viewModel {
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
        val viewModel = viewModel {
            PdfKeywordSearchOutcome.BlankQuery
        }
        viewModel.onQueryChanged("   ")
        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(PdfKeywordSearchPhase.EmptyQuery, viewModel.uiState.value.phase)
    }

    @Test
    fun empty_hits_map_to_no_matches_with_submitted_query() = runTest {
        val viewModel = viewModel {
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
        val viewModel = viewModel {
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
        val viewModel = viewModel {
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

    @Test
    fun clear_derived_data_drops_results_so_why_cannot_cite_deleted_excerpts() = runTest {
        var pageCount = 2
        val viewModel = viewModel(
            search = {
                PdfKeywordSearchOutcome.Matches(
                    query = "meet",
                    hits = listOf(sampleHit()),
                    limitReached = false,
                )
            },
            readiness = {
                PdfKeywordSearchReadiness(
                    pageCount = pageCount,
                    documentCount = if (pageCount == 0) 0 else 1,
                )
            },
        )
        viewModel.onQueryChanged("meet")
        viewModel.onSearch()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.phase is PdfKeywordSearchPhase.Results)

        pageCount = 0
        viewModel.onDerivedDataCleared()
        advanceUntilIdle()

        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("", viewModel.uiState.value.query)
        val readiness = viewModel.uiState.value.readiness as PdfKeywordSearchReadinessUi.Ready
        assertEquals(0, readiness.snapshot.pageCount)
        assertEquals(0, readiness.snapshot.documentCount)
    }

    @Test
    fun clear_query_empties_field_and_returns_idle() = runTest {
        val viewModel = viewModel {
            PdfKeywordSearchOutcome.Matches(
                query = "meet mira",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
        viewModel.onQueryChanged("meet mira")
        viewModel.onSearch()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canClearQuery)

        viewModel.onQueryCleared()
        assertEquals("", viewModel.uiState.value.query)
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.canClearQuery)
        assertFalse(viewModel.uiState.value.canSubmitSearch)
    }

    @Test
    fun clear_query_is_ignored_while_searching() = runTest {
        val deferred = CompletableDeferred<PdfKeywordSearchOutcome>()
        val viewModel = viewModel { deferred.await() }
        viewModel.onQueryChanged("meet")
        viewModel.onSearch()
        dispatcher.scheduler.runCurrent()
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.canClearQuery)

        viewModel.onQueryCleared()
        assertEquals("meet", viewModel.uiState.value.query)
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)

        deferred.complete(
            PdfKeywordSearchOutcome.Matches(
                query = "meet",
                hits = listOf(sampleHit()),
                limitReached = false,
            ),
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.phase is PdfKeywordSearchPhase.Results)
    }

    @Test
    fun cancel_search_returns_idle_keeps_query_and_ignores_late_completion() = runTest {
        val deferred = CompletableDeferred<PdfKeywordSearchOutcome>()
        val viewModel = viewModel { deferred.await() }
        viewModel.onQueryChanged("meet mira")
        viewModel.onSearch()
        dispatcher.scheduler.runCurrent()
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.canCancelSearch)
        assertFalse(viewModel.uiState.value.canSubmitSearch)

        viewModel.onSearchCancelled()
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("meet mira", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.canCancelSearch)
        assertTrue(viewModel.uiState.value.canSubmitSearch)
        assertTrue(viewModel.uiState.value.canClearQuery)

        deferred.complete(
            PdfKeywordSearchOutcome.Matches(
                query = "meet mira",
                hits = listOf(sampleHit()),
                limitReached = false,
            ),
        )
        advanceUntilIdle()
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("meet mira", viewModel.uiState.value.query)
    }

    @Test
    fun cancel_search_is_ignored_when_not_searching() = runTest {
        val viewModel = viewModel {
            PdfKeywordSearchOutcome.Matches(
                query = "meet",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
        viewModel.onQueryChanged("meet")
        viewModel.onSearchCancelled()
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("meet", viewModel.uiState.value.query)

        viewModel.onSearch()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.phase is PdfKeywordSearchPhase.Results)
        viewModel.onSearchCancelled()
        assertTrue(viewModel.uiState.value.phase is PdfKeywordSearchPhase.Results)
    }

    @Test
    fun fast_search_holds_searching_for_minimum_visible_time() = runTest {
        val viewModel = viewModel(minSearchingVisibleMs = 500) {
            PdfKeywordSearchOutcome.Matches(
                query = "meet",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
        viewModel.onQueryChanged("meet")
        viewModel.onSearch()
        dispatcher.scheduler.runCurrent()
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.canCancelSearch)

        advanceTimeBy(499)
        dispatcher.scheduler.runCurrent()
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.phase is PdfKeywordSearchPhase.Results)
    }

    @Test
    fun cancel_during_minimum_visible_hold_returns_idle() = runTest {
        val viewModel = viewModel(minSearchingVisibleMs = 500) {
            PdfKeywordSearchOutcome.Matches(
                query = "meet",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
        viewModel.onQueryChanged("meet")
        viewModel.onSearch()
        dispatcher.scheduler.runCurrent()
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)

        viewModel.onSearchCancelled()
        advanceUntilIdle()
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("meet", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.canCancelSearch)
    }

    @Test
    fun cancelled_search_does_not_apply_results_after_idle() = runTest {
        val deferred = CompletableDeferred<PdfKeywordSearchOutcome>()
        val viewModel = viewModel(minSearchingVisibleMs = 0) { deferred.await() }
        viewModel.onQueryChanged("meet")
        viewModel.onSearch()
        dispatcher.scheduler.runCurrent()
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)

        viewModel.onSearchCancelled()
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)

        deferred.complete(
            PdfKeywordSearchOutcome.Matches(
                query = "meet",
                hits = listOf(sampleHit()),
                limitReached = false,
            ),
        )
        advanceUntilIdle()
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("meet", viewModel.uiState.value.query)
    }

    @Test
    fun clear_derived_data_ignores_in_flight_search_completion() = runTest {
        val deferred = CompletableDeferred<PdfKeywordSearchOutcome>()
        val viewModel = viewModel { deferred.await() }

        viewModel.onQueryChanged("meet")
        viewModel.onSearch()
        dispatcher.scheduler.runCurrent()
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)

        viewModel.onDerivedDataCleared()
        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals("", viewModel.uiState.value.query)

        deferred.complete(
            PdfKeywordSearchOutcome.Matches(
                query = "meet",
                hits = listOf(sampleHit()),
                limitReached = false,
            ),
        )
        advanceUntilIdle()

        assertEquals(PdfKeywordSearchPhase.Idle, viewModel.uiState.value.phase)
    }

    @Test
    fun screen_visible_refreshes_readiness_counts() = runTest {
        var pageCount = 0
        val viewModel = viewModel(
            search = { PdfKeywordSearchOutcome.BlankQuery },
            readiness = {
                PdfKeywordSearchReadiness(pageCount = pageCount, documentCount = if (pageCount == 0) 0 else 1)
            },
        )
        advanceUntilIdle()
        assertEquals(
            PdfKeywordSearchReadinessUi.Ready(PdfKeywordSearchReadiness(0, 0)),
            viewModel.uiState.value.readiness,
        )

        pageCount = 3
        viewModel.onScreenVisible()
        advanceUntilIdle()

        val readiness = viewModel.uiState.value.readiness as PdfKeywordSearchReadinessUi.Ready
        assertEquals(3, readiness.snapshot.pageCount)
        assertEquals(1, readiness.snapshot.documentCount)
    }

    @Test
    fun readiness_load_failure_is_recoverable() = runTest {
        val viewModel = viewModel(
            search = { PdfKeywordSearchOutcome.BlankQuery },
            readiness = { error("simulated readiness failure") },
        )
        advanceUntilIdle()
        assertEquals(PdfKeywordSearchReadinessUi.CouldNotLoad, viewModel.uiState.value.readiness)
    }

    @Test
    fun can_submit_search_requires_non_blank_query_and_idle_from_searching() = runTest {
        val deferred = CompletableDeferred<PdfKeywordSearchOutcome>()
        val viewModel = viewModel { deferred.await() }

        assertFalse(viewModel.uiState.value.canSubmitSearch)

        viewModel.onQueryChanged("meet")
        assertTrue(viewModel.uiState.value.canSubmitSearch)

        viewModel.onSearch()
        dispatcher.scheduler.runCurrent()
        assertEquals(PdfKeywordSearchPhase.Searching, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.canSubmitSearch)

        deferred.complete(
            PdfKeywordSearchOutcome.Matches(
                query = "meet",
                hits = listOf(sampleHit()),
                limitReached = false,
            ),
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canSubmitSearch)
    }

    @Test
    fun open_original_shows_preview_for_ready_render() = runTest {
        val pixels = intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        val viewModel = viewModel(
            open = {
                PdfPagePreviewRenderResult.Ready(
                    documentLabel = it.label,
                    pageNumber = it.pageNumber,
                    pageCount = 2,
                    widthPx = 2,
                    heightPx = 2,
                    argb8888 = pixels,
                )
            },
        ) {
            PdfKeywordSearchOutcome.Matches(
                query = "meet mira",
                hits = listOf(sampleHit(pageNumber = 2)),
                limitReached = false,
            )
        }

        viewModel.onQueryChanged("meet mira")
        viewModel.onSearch()
        advanceUntilIdle()
        val hit = (viewModel.uiState.value.phase as PdfKeywordSearchPhase.Results).hits.first()
        viewModel.onOpenOriginalPdf(hit)
        advanceUntilIdle()

        val preview = viewModel.uiState.value.originalPreview
        assertEquals(2, preview?.pageNumber)
        assertEquals(2, preview?.pageCount)
        assertEquals("source-1", preview?.sourceId)
        assertEquals("asset-1", preview?.sourceAssetKey)
        assertEquals(PdfOpenFeedbackUi.None, viewModel.uiState.value.openFeedback)
    }

    @Test
    fun open_original_maps_source_unavailable_to_feedback() = runTest {
        val viewModel = viewModel(
            open = { PdfPagePreviewRenderResult.SourceUnavailable },
        ) {
            PdfKeywordSearchOutcome.Matches(
                query = "meet",
                hits = listOf(sampleHit()),
                limitReached = false,
            )
        }
        viewModel.onQueryChanged("meet")
        viewModel.onSearch()
        advanceUntilIdle()
        viewModel.onOpenOriginalPdf(sampleHit())
        advanceUntilIdle()
        assertEquals(PdfOpenFeedbackUi.SourceUnavailable, viewModel.uiState.value.openFeedback)
        assertEquals(null, viewModel.uiState.value.originalPreview)
    }

    private fun viewModel(
        readiness: suspend () -> PdfKeywordSearchReadiness = {
            PdfKeywordSearchReadiness(pageCount = 0, documentCount = 0)
        },
        open: suspend (PdfKeywordSearchHit) -> PdfPagePreviewRenderResult = {
            PdfPagePreviewRenderResult.CouldNotOpen
        },
        minSearchingVisibleMs: Long = 0L,
        monotonicMs: () -> Long = { 0L },
        search: suspend (String) -> PdfKeywordSearchOutcome,
    ) = PdfKeywordSearchViewModel(
        searchPdfKeyword = search,
        loadReadiness = readiness,
        openPersistedPdfForViewing = open,
        minSearchingVisibleMs = minSearchingVisibleMs,
        monotonicMs = monotonicMs,
    )

    private fun sampleHit(label: String = "fixture.pdf", pageNumber: Int = 1) = PdfKeywordSearchHit(
        recall = CanonicalRecallTestFixtures.keywordRecall(
            label = label,
            pageNumber = pageNumber,
        ),
    )
}
