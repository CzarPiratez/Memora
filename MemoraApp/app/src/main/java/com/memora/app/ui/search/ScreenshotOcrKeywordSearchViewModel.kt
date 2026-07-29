package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.images.LoadPersistedScreenshotOcrKeywordSearchReadiness
import com.memora.app.application.images.ScreenshotOcrKeywordSearchHit
import com.memora.app.application.images.ScreenshotOcrKeywordSearchOutcome
import com.memora.app.application.images.ScreenshotOcrKeywordSearchReadiness
import com.memora.app.application.images.SearchPersistedScreenshotOcrText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScreenshotOcrKeywordSearchUiState(
    val query: String = "",
    val phase: ScreenshotOcrKeywordSearchPhase = ScreenshotOcrKeywordSearchPhase.Idle,
    val readiness: ScreenshotOcrKeywordSearchReadinessUi =
        ScreenshotOcrKeywordSearchReadinessUi.Loading,
) {
    val canSubmitSearch: Boolean
        get() = phase !is ScreenshotOcrKeywordSearchPhase.Searching && query.isNotBlank()

    val canClearQuery: Boolean
        get() = query.isNotBlank() && phase !is ScreenshotOcrKeywordSearchPhase.Searching

    val canCancelSearch: Boolean
        get() = phase is ScreenshotOcrKeywordSearchPhase.Searching
}

sealed interface ScreenshotOcrKeywordSearchReadinessUi {
    data object Loading : ScreenshotOcrKeywordSearchReadinessUi

    data object CouldNotLoad : ScreenshotOcrKeywordSearchReadinessUi

    data class Ready(
        val snapshot: ScreenshotOcrKeywordSearchReadiness,
    ) : ScreenshotOcrKeywordSearchReadinessUi
}

sealed interface ScreenshotOcrKeywordSearchPhase {
    data object Idle : ScreenshotOcrKeywordSearchPhase

    data object EmptyQuery : ScreenshotOcrKeywordSearchPhase

    data object Searching : ScreenshotOcrKeywordSearchPhase

    data class Results(
        val query: String,
        val hits: List<ScreenshotOcrKeywordSearchHit>,
        val limitReached: Boolean,
    ) : ScreenshotOcrKeywordSearchPhase {
        init {
            require(query.isNotBlank()) { "Results need the search query for Explain Mode." }
            require(hits.isNotEmpty()) { "Results need at least one hit." }
        }
    }

    data class NoMatches(
        val query: String,
    ) : ScreenshotOcrKeywordSearchPhase {
        init {
            require(query.isNotBlank()) { "No-matches needs the submitted search query." }
        }
    }

    data class NothingSavedToSearch(
        val query: String,
    ) : ScreenshotOcrKeywordSearchPhase {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved needs the submitted search query."
            }
        }
    }

    data object SearchCouldNotFinish : ScreenshotOcrKeywordSearchPhase
}

@HiltViewModel
class ScreenshotOcrKeywordSearchViewModel(
    private val searchPersistedScreenshotOcrText: suspend (String) -> ScreenshotOcrKeywordSearchOutcome,
    private val loadReadiness: suspend () -> ScreenshotOcrKeywordSearchReadiness,
    private val minSearchingVisibleMs: Long = DEFAULT_MIN_SEARCHING_VISIBLE_MS,
    private val monotonicMs: () -> Long = { System.nanoTime() / 1_000_000L },
) : ViewModel() {
    @Inject
    constructor(
        searchPersistedScreenshotOcrText: SearchPersistedScreenshotOcrText,
        loadPersistedScreenshotOcrKeywordSearchReadiness:
            LoadPersistedScreenshotOcrKeywordSearchReadiness,
    ) : this(
        searchPersistedScreenshotOcrText = { rawQuery -> searchPersistedScreenshotOcrText(rawQuery) },
        loadReadiness = { loadPersistedScreenshotOcrKeywordSearchReadiness() },
    )

    private val mutableUiState = MutableStateFlow(ScreenshotOcrKeywordSearchUiState())
    val uiState: StateFlow<ScreenshotOcrKeywordSearchUiState> = mutableUiState.asStateFlow()

    private val searchGeneration = AtomicInteger(0)
    private val readinessGeneration = AtomicInteger(0)
    private var searchJob: Job? = null

    init {
        refreshReadiness()
    }

    fun onScreenVisible() {
        refreshReadiness()
    }

    fun onQueryChanged(value: String) {
        val current = mutableUiState.value
        if (value == current.query) return
        searchGeneration.incrementAndGet()
        mutableUiState.value = current.copy(
            query = value,
            phase = ScreenshotOcrKeywordSearchPhase.Idle,
        )
    }

    fun onQueryCleared() {
        val current = mutableUiState.value
        if (!current.canClearQuery) return
        onQueryChanged("")
    }

    fun onSearch() {
        val query = mutableUiState.value.query
        val generation = searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                phase = ScreenshotOcrKeywordSearchPhase.Searching,
            )
            val startedAtMs = monotonicMs()
            val outcome = try {
                searchPersistedScreenshotOcrText(query)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation != searchGeneration.get()) return@launch
                mutableUiState.update { state ->
                    if (generation != searchGeneration.get()) return@update state
                    if (state.phase !is ScreenshotOcrKeywordSearchPhase.Searching) {
                        return@update state
                    }
                    state.copy(phase = ScreenshotOcrKeywordSearchPhase.SearchCouldNotFinish)
                }
                return@launch
            }
            if (generation != searchGeneration.get()) return@launch

            val elapsedMs = monotonicMs() - startedAtMs
            val remainingMs = minSearchingVisibleMs - elapsedMs
            if (remainingMs > 0) {
                delay(remainingMs)
            }
            if (generation != searchGeneration.get()) return@launch

            val nextPhase = when (outcome) {
                ScreenshotOcrKeywordSearchOutcome.BlankQuery ->
                    ScreenshotOcrKeywordSearchPhase.EmptyQuery
                is ScreenshotOcrKeywordSearchOutcome.NothingSavedToSearch ->
                    ScreenshotOcrKeywordSearchPhase.NothingSavedToSearch(query = outcome.query)
                is ScreenshotOcrKeywordSearchOutcome.Matches -> if (outcome.hits.isEmpty()) {
                    ScreenshotOcrKeywordSearchPhase.NoMatches(query = outcome.query)
                } else {
                    ScreenshotOcrKeywordSearchPhase.Results(
                        query = outcome.query,
                        hits = outcome.hits,
                        limitReached = outcome.limitReached,
                    )
                }
            }
            mutableUiState.update { state ->
                if (generation != searchGeneration.get()) return@update state
                if (state.phase !is ScreenshotOcrKeywordSearchPhase.Searching) return@update state
                state.copy(phase = nextPhase)
            }
        }
    }

    fun onSearchCancelled() {
        searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = null
        mutableUiState.update { state ->
            if (state.phase !is ScreenshotOcrKeywordSearchPhase.Searching) return@update state
            state.copy(phase = ScreenshotOcrKeywordSearchPhase.Idle)
        }
    }

    fun onDerivedDataCleared() {
        searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = null
        mutableUiState.value = mutableUiState.value.copy(
            query = "",
            phase = ScreenshotOcrKeywordSearchPhase.Idle,
            readiness = ScreenshotOcrKeywordSearchReadinessUi.Ready(
                ScreenshotOcrKeywordSearchReadiness(screenshotCount = 0),
            ),
        )
        refreshReadiness()
    }

    private fun refreshReadiness() {
        val generation = readinessGeneration.incrementAndGet()
        mutableUiState.value = mutableUiState.value.copy(
            readiness = ScreenshotOcrKeywordSearchReadinessUi.Loading,
        )
        viewModelScope.launch {
            val snapshot = try {
                loadReadiness()
            } catch (_: Exception) {
                if (generation != readinessGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    readiness = ScreenshotOcrKeywordSearchReadinessUi.CouldNotLoad,
                )
                return@launch
            }
            if (generation != readinessGeneration.get()) return@launch
            mutableUiState.value = mutableUiState.value.copy(
                readiness = ScreenshotOcrKeywordSearchReadinessUi.Ready(snapshot),
            )
        }
    }

    companion object {
        const val DEFAULT_MIN_SEARCHING_VISIBLE_MS = 700L
    }
}
