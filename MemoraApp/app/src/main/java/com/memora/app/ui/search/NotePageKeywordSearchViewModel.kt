package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.memory.SearchMemoryEvidence
import com.memora.app.application.notes.ExternalUrlLauncher
import com.memora.app.application.notes.LoadPersistedNotePageKeywordSearchReadiness
import com.memora.app.application.notes.NoteMemoryEvidenceKeywordAdapter
import com.memora.app.application.notes.NotePageKeywordSearchHit
import com.memora.app.application.notes.NotePageKeywordSearchOutcome
import com.memora.app.application.notes.NotePageKeywordSearchReadiness
import com.memora.app.application.notes.OpenPersistedNotePageInOneNote
import com.memora.app.application.notes.OpenPersistedNotePageResult
import com.memora.app.domain.asset.AssetType
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
    val openFeedback: NotePageOpenFeedbackUi = NotePageOpenFeedbackUi.None,
) {
    val canSubmitSearch: Boolean
        get() = phase !is NotePageKeywordSearchPhase.Searching &&
            query.isNotBlank() &&
            openFeedback !is NotePageOpenFeedbackUi.Opening

    val canClearQuery: Boolean
        get() = query.isNotBlank() &&
            phase !is NotePageKeywordSearchPhase.Searching &&
            openFeedback !is NotePageOpenFeedbackUi.Opening

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

sealed interface NotePageOpenFeedbackUi {
    data object None : NotePageOpenFeedbackUi

    data object Opening : NotePageOpenFeedbackUi

    data object SourceUnavailable : NotePageOpenFeedbackUi

    data object CouldNotOpen : NotePageOpenFeedbackUi
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

/**
 * Note keyword Find — MIG-07 cutover to [SearchMemoryEvidence]
 * (AssetType.NOTE).
 *
 * L4 `SearchPersistedNotePageText` retired; UI models preserved via adapter.
 * Open-original remains source-identity based (OneNote web/client URLs).
 */
@HiltViewModel
class NotePageKeywordSearchViewModel(
    private val searchPersistedNotePageText: suspend (String) -> NotePageKeywordSearchOutcome,
    private val loadReadiness: suspend () -> NotePageKeywordSearchReadiness,
    private val openPersistedNotePage: suspend (String, String) -> OpenPersistedNotePageResult,
    private val launchOneNoteOriginal: (webUrl: String?, clientUrl: String?) -> Boolean,
    private val minSearchingVisibleMs: Long = DEFAULT_MIN_SEARCHING_VISIBLE_MS,
    private val monotonicMs: () -> Long = { System.nanoTime() / 1_000_000L },
) : ViewModel() {
    @Inject
    constructor(
        searchMemoryEvidence: SearchMemoryEvidence,
        loadPersistedNotePageKeywordSearchReadiness: LoadPersistedNotePageKeywordSearchReadiness,
        openPersistedNotePageInOneNote: OpenPersistedNotePageInOneNote,
        externalUrlLauncher: ExternalUrlLauncher,
    ) : this(
        searchPersistedNotePageText = { rawQuery ->
            NoteMemoryEvidenceKeywordAdapter.toNoteOutcome(
                searchMemoryEvidence(
                    rawQuery = rawQuery,
                    assetType = AssetType.NOTE,
                ),
            )
        },
        loadReadiness = { loadPersistedNotePageKeywordSearchReadiness() },
        openPersistedNotePage = { sourceId, sourceAssetKey ->
            openPersistedNotePageInOneNote(sourceId, sourceAssetKey)
        },
        launchOneNoteOriginal = { webUrl, clientUrl ->
            externalUrlLauncher.launchOneNoteOriginal(webUrl, clientUrl)
        },
    )

    private val mutableUiState = MutableStateFlow(NotePageKeywordSearchUiState())
    val uiState: StateFlow<NotePageKeywordSearchUiState> = mutableUiState.asStateFlow()

    private val searchGeneration = AtomicInteger(0)
    private val readinessGeneration = AtomicInteger(0)
    private val openGeneration = AtomicInteger(0)
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
            openFeedback = NotePageOpenFeedbackUi.None,
        )
    }

    fun onQueryCleared() {
        val current = mutableUiState.value
        if (!current.canClearQuery) return
        onQueryChanged("")
    }

    fun onSearch() {
        if (mutableUiState.value.openFeedback is NotePageOpenFeedbackUi.Opening) return
        val query = mutableUiState.value.query
        val generation = searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                phase = NotePageKeywordSearchPhase.Searching,
                openFeedback = NotePageOpenFeedbackUi.None,
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

    fun onOpenOriginalNote(hit: NotePageKeywordSearchHit) {
        if (mutableUiState.value.openFeedback is NotePageOpenFeedbackUi.Opening) return
        val generation = openGeneration.incrementAndGet()
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                openFeedback = NotePageOpenFeedbackUi.Opening,
            )
            val outcome = try {
                openPersistedNotePage(hit.sourceId, hit.sourceAssetKey)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation != openGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    openFeedback = NotePageOpenFeedbackUi.CouldNotOpen,
                )
                return@launch
            }
            if (generation != openGeneration.get()) return@launch
            mutableUiState.value = when (outcome) {
                is OpenPersistedNotePageResult.Ready -> {
                    val launched = try {
                        launchOneNoteOriginal(outcome.webUrl, outcome.clientUrl)
                    } catch (_: Exception) {
                        false
                    }
                    mutableUiState.value.copy(
                        openFeedback = if (launched) {
                            NotePageOpenFeedbackUi.None
                        } else {
                            NotePageOpenFeedbackUi.CouldNotOpen
                        },
                    )
                }
                OpenPersistedNotePageResult.SourceUnavailable ->
                    mutableUiState.value.copy(
                        openFeedback = NotePageOpenFeedbackUi.SourceUnavailable,
                    )
                OpenPersistedNotePageResult.CouldNotOpen ->
                    mutableUiState.value.copy(
                        openFeedback = NotePageOpenFeedbackUi.CouldNotOpen,
                    )
            }
        }
    }

    fun onOpenFeedbackDismissed() {
        if (mutableUiState.value.openFeedback is NotePageOpenFeedbackUi.Opening) return
        mutableUiState.value = mutableUiState.value.copy(openFeedback = NotePageOpenFeedbackUi.None)
    }

    fun onDerivedDataCleared() {
        searchGeneration.incrementAndGet()
        openGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = null
        mutableUiState.value = mutableUiState.value.copy(
            query = "",
            phase = NotePageKeywordSearchPhase.Idle,
            readiness = NotePageKeywordSearchReadinessUi.Ready(
                NotePageKeywordSearchReadiness(noteCount = 0),
            ),
            openFeedback = NotePageOpenFeedbackUi.None,
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
