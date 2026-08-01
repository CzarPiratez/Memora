package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.notes.LoadPersistedNotePageKeywordSearchReadiness
import com.memora.app.application.notes.NotePageKeywordSearchHit
import com.memora.app.application.notes.NotePageKeywordSearchOutcome
import com.memora.app.application.notes.NotePageKeywordSearchReadiness
import com.memora.app.application.notes.SearchPersistedNotePageText
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

data class NotePageKeywordSearchUiState(
    val query: String = "",
    val phase: NotePageKeywordSearchPhase = NotePageKeywordSearchPhase.Idle,
    val readiness: NotePageKeywordSearchReadinessUi = NotePageKeywordSearchReadinessUi.Loading,
) {
    val canSubmitSearch: Boolean
        get() = phase !is NotePageKeywordSearchPhase.Searching && query.isNotBlank()

    val canClearQuery: Boolean
        get() = query.isNotBlank() && phase !is NotePageKeywordSearchPhase.Searching

    val canCancelSearch: Boolean
        get() = phase is NotePageKeywordSearchPhase.Searching
}

sealed interface NotePageKeywordSearchReadinessUi {
    data object Loading : NotePageKeywordSearchReadinessUi

    data object CouldNotLoad : NotePageKeywordSearchReadinessUi

    data class Ready(
        val snapshot: NotePageKeywordSearchReadiness,
    ) : NotePageKeywordSearchReadinessUi
}

sealed interface NotePageKeywordSearchPhase {
    data object Idle : NotePageKeywordSearchPhase

    data object EmptyQuery : NotePageKeywordSearchPhase

    data object Searching : NotePageKeywordSearchPhase

    data class Results(
        val query: String,
        val hits: List<NotePageKeywordSearchHit>,
        val limitReached: Boolean,
    ) : NotePageKeywordSearchPhase {
        init {
            require(query.isNotBlank()) { "Results need the search query for Explain Mode." }
            require(hits.isNotEmpty()) { "Results need at least one hit." }
        }
    }

    data class NoMatches(
        val query: String,
    ) : NotePageKeywordSearchPhase {
        init {
            require(query.isNotBlank()) { "No-matches needs the submitted search query." }
        }
    }

    data class NothingSavedToSearch(
        val query: String,
    ) : NotePageKeywordSearchPhase {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved needs the submitted search query."
            }
        }
    }

    data object SearchCouldNotFinish : NotePageKeywordSearchPhase
}

@HiltViewModel
class NotePageKeywordSearchViewModel(
    private val searchPersistedNotePageText: suspend (String) -> NotePageKeywordSearchOutcome,
    private val loadReadiness: suspend () -> NotePageKeywordSearchReadiness,
    private val minSearchingVisibleMs: Long = DEFAULT_MIN_SEARCHING_VISIBLE_MS,
    private val monotonicMs: () -> Long = { System.nanoTime() / 1_000_000L },
) : ViewModel() {
    @Inject
    constructor(
        searchPersistedNotePageText: SearchPersistedNotePageText,
        loadPersistedNotePageKeywordSearchReadiness: LoadPersistedNotePageKeywordSearchReadiness,
    ) : this(
        searchPersistedNotePageText = { rawQuery -> searchPersistedNotePageText(rawQuery) },
        loadReadiness = { loadPersistedNotePageKeywordSearchReadiness() },
    )

    private val mutableUiState = MutableStateFlow(NotePageKeywordSearchUiState())
    val uiState: StateFlow<NotePageKeywordSearchUiState> = mutableUiState.asStateFlow()

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
            phase = NotePageKeywordSearchPhase.Idle,
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
                phase = NotePageKeywordSearchPhase.Searching,
            )
            val startedAtMs = monotonicMs()
            val outcome = try {
                searchPersistedNotePageText(query)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation != searchGeneration.get()) return@launch
                mutableUiState.update { state ->
                    if (generation != searchGeneration.get()) return@update state
                    if (state.phase !is NotePageKeywordSearchPhase.Searching) {
                        return@update state
                    }
                    state.copy(phase = NotePageKeywordSearchPhase.SearchCouldNotFinish)
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
                NotePageKeywordSearchOutcome.BlankQuery ->
                    NotePageKeywordSearchPhase.EmptyQuery
                is NotePageKeywordSearchOutcome.NothingSavedToSearch ->
                    NotePageKeywordSearchPhase.NothingSavedToSearch(query = outcome.query)
                is NotePageKeywordSearchOutcome.Matches -> if (outcome.hits.isEmpty()) {
                    NotePageKeywordSearchPhase.NoMatches(query = outcome.query)
                } else {
                    NotePageKeywordSearchPhase.Results(
                        query = outcome.query,
                        hits = outcome.hits,
                        limitReached = outcome.limitReached,
                    )
                }
            }
            mutableUiState.update { state ->
                if (generation != searchGeneration.get()) return@update state
                if (state.phase !is NotePageKeywordSearchPhase.Searching) return@update state
                state.copy(phase = nextPhase)
            }
        }
    }

    fun onSearchCancelled() {
        searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = null
        mutableUiState.update { state ->
            if (state.phase !is NotePageKeywordSearchPhase.Searching) return@update state
            state.copy(phase = NotePageKeywordSearchPhase.Idle)
        }
    }

    fun onDerivedDataCleared() {
        searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = null
        mutableUiState.value = mutableUiState.value.copy(
            query = "",
            phase = NotePageKeywordSearchPhase.Idle,
            readiness = NotePageKeywordSearchReadinessUi.Ready(
                NotePageKeywordSearchReadiness(noteCount = 0),
            ),
        )
        refreshReadiness()
    }

    private fun refreshReadiness() {
        val generation = readinessGeneration.incrementAndGet()
        mutableUiState.value = mutableUiState.value.copy(
            readiness = NotePageKeywordSearchReadinessUi.Loading,
        )
        viewModelScope.launch {
            val snapshot = try {
                loadReadiness()
            } catch (_: Exception) {
                if (generation != readinessGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    readiness = NotePageKeywordSearchReadinessUi.CouldNotLoad,
                )
                return@launch
            }
            if (generation != readinessGeneration.get()) return@launch
            mutableUiState.value = mutableUiState.value.copy(
                readiness = NotePageKeywordSearchReadinessUi.Ready(snapshot),
            )
        }
    }

    companion object {
        const val DEFAULT_MIN_SEARCHING_VISIBLE_MS = 700L
    }
}
