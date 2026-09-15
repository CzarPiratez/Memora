package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.memory.CanonicalRecall
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
    /**
     * An open in flight no longer locks the screen. Opening a note waits on
     * Microsoft Graph, and freezing the search box for that long because
     * someone tapped a result is not a state the person asked for. Starting a
     * search abandons the open instead.
     */
    val canSubmitSearch: Boolean
        get() = phase !is NotePageKeywordSearchPhase.Searching && query.isNotBlank()

    val canClearQuery: Boolean
        get() = query.isNotBlank() && phase !is NotePageKeywordSearchPhase.Searching

    val canCancelSearch: Boolean
        get() = phase is NotePageKeywordSearchPhase.Searching

    /** What [target]'s own card should show. Every other card stays idle. */
    fun openStateFor(target: FindOpenTarget): FindCardOpenState = when (openFeedback) {
        NotePageOpenFeedbackUi.None -> FindCardOpenState.IDLE
        is NotePageOpenFeedbackUi.Opening ->
            if (openFeedback.target == target) FindCardOpenState.OPENING else FindCardOpenState.IDLE
        is NotePageOpenFeedbackUi.SourceUnavailable ->
            if (openFeedback.target == target) {
                FindCardOpenState.SOURCE_UNAVAILABLE
            } else {
                FindCardOpenState.IDLE
            }
        is NotePageOpenFeedbackUi.CouldNotOpen ->
            if (openFeedback.target == target) {
                FindCardOpenState.COULD_NOT_OPEN
            } else {
                FindCardOpenState.IDLE
            }
    }
}

sealed interface NotePageKeywordSearchReadinessUi {
    data object Loading : NotePageKeywordSearchReadinessUi

    data object CouldNotLoad : NotePageKeywordSearchReadinessUi

    data class Ready(
        val snapshot: NotePageKeywordSearchReadiness,
    ) : NotePageKeywordSearchReadinessUi
}

/**
 * Open-original feedback, always attached to the card it came from.
 *
 * @see FindOpenTarget for why the screen-level flag was wrong.
 */
sealed interface NotePageOpenFeedbackUi {
    data object None : NotePageOpenFeedbackUi

    data class Opening(val target: FindOpenTarget) : NotePageOpenFeedbackUi

    data class SourceUnavailable(val target: FindOpenTarget) : NotePageOpenFeedbackUi

    data class CouldNotOpen(val target: FindOpenTarget) : NotePageOpenFeedbackUi
}

/** The card identity for a note keyword hit. */
fun NotePageKeywordSearchHit.openTarget(): FindOpenTarget = FindOpenTarget(
    sourceId = sourceId,
    sourceAssetKey = sourceAssetKey,
)

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
 * Note keyword Find via [CanonicalRecall] (KEYWORD path; AssetType.NOTE).
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
        canonicalRecall: CanonicalRecall,
        loadPersistedNotePageKeywordSearchReadiness: LoadPersistedNotePageKeywordSearchReadiness,
        openPersistedNotePageInOneNote: OpenPersistedNotePageInOneNote,
        externalUrlLauncher: ExternalUrlLauncher,
    ) : this(
        searchPersistedNotePageText = { rawQuery ->
            NoteMemoryEvidenceKeywordAdapter.toNoteOutcome(
                canonicalRecall(
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
    private var openJob: Job? = null

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
        val query = mutableUiState.value.query

        // A new search replaces the list the open belonged to, so abandon it
        // rather than leaving a Graph call running against a card that is gone.
        openGeneration.incrementAndGet()
        openJob?.cancel()
        openJob = null

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

    /**
     * A tap while another open is in flight supersedes it rather than being
     * swallowed. Ignoring the second tap reads as a dead button — a note open
     * can sit on Graph for seconds, and during that window the person has
     * clearly told us they want a different page.
     */
    fun onOpenOriginalNote(hit: NotePageKeywordSearchHit) {
        val target = hit.openTarget()
        val generation = openGeneration.incrementAndGet()
        openJob?.cancel()
        openJob = viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                openFeedback = NotePageOpenFeedbackUi.Opening(target),
            )
            val outcome = try {
                openPersistedNotePage(hit.sourceId, hit.sourceAssetKey)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation != openGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    openFeedback = NotePageOpenFeedbackUi.CouldNotOpen(target),
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
                            NotePageOpenFeedbackUi.CouldNotOpen(target)
                        },
                    )
                }
                OpenPersistedNotePageResult.SourceUnavailable ->
                    mutableUiState.value.copy(
                        openFeedback = NotePageOpenFeedbackUi.SourceUnavailable(target),
                    )
                OpenPersistedNotePageResult.CouldNotOpen ->
                    mutableUiState.value.copy(
                        openFeedback = NotePageOpenFeedbackUi.CouldNotOpen(target),
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
        openJob?.cancel()
        openJob = null
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
