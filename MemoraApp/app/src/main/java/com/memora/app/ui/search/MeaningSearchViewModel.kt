package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.intelligence.LoadMeaningSearchReadiness
import com.memora.app.application.intelligence.MeaningOpenOriginalResult
import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.application.intelligence.MeaningSearchReadiness
import com.memora.app.application.intelligence.OpenMeaningSearchOriginal
import com.memora.app.application.intelligence.SearchAssetMemoriesByMeaning
import com.memora.app.application.notes.ExternalUrlLauncher
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

data class MeaningSearchUiState(
    val query: String = "",
    val phase: MeaningSearchPhase = MeaningSearchPhase.Idle,
    val readiness: MeaningSearchReadinessUi = MeaningSearchReadinessUi.Loading,
    val openFeedback: MeaningOpenFeedbackUi = MeaningOpenFeedbackUi.None,
    val originalPreview: MeaningOriginalPreviewUi? = null,
) {
    val canSubmitSearch: Boolean
        get() = phase !is MeaningSearchPhase.Searching &&
            query.isNotBlank() &&
            openFeedback !is MeaningOpenFeedbackUi.Opening

    val canClearQuery: Boolean
        get() = query.isNotBlank() &&
            phase !is MeaningSearchPhase.Searching &&
            openFeedback !is MeaningOpenFeedbackUi.Opening

    val canCancelSearch: Boolean
        get() = phase is MeaningSearchPhase.Searching

    val canOpenOriginal: Boolean
        get() = openFeedback !is MeaningOpenFeedbackUi.Opening
}

sealed interface MeaningSearchReadinessUi {
    data object Loading : MeaningSearchReadinessUi

    data object CouldNotLoad : MeaningSearchReadinessUi

    data class Ready(
        val snapshot: MeaningSearchReadiness.Ready,
    ) : MeaningSearchReadinessUi

    data class EngineUnavailable(
        val reason: String,
    ) : MeaningSearchReadinessUi {
        init {
            require(reason.isNotBlank())
        }
    }
}

sealed interface MeaningOpenFeedbackUi {
    data object None : MeaningOpenFeedbackUi

    data object Opening : MeaningOpenFeedbackUi

    data object SourceUnavailable : MeaningOpenFeedbackUi

    data object CouldNotOpen : MeaningOpenFeedbackUi
}

sealed interface MeaningOriginalPreviewUi {
    data class Screenshot(val preview: ScreenshotOriginalPreviewUi) : MeaningOriginalPreviewUi

    data class Photo(val preview: PhotoOriginalPreviewUi) : MeaningOriginalPreviewUi

    data class Pdf(val preview: PdfOriginalPreviewUi) : MeaningOriginalPreviewUi
}

sealed interface MeaningSearchPhase {
    data object Idle : MeaningSearchPhase

    data object EmptyQuery : MeaningSearchPhase

    data object Searching : MeaningSearchPhase

    data class Results(
        val query: String,
        val hits: List<MeaningSearchHit>,
        val limitReached: Boolean,
    ) : MeaningSearchPhase {
        init {
            require(query.isNotBlank())
            require(hits.isNotEmpty())
        }
    }

    data class NoMatches(
        val query: String,
    ) : MeaningSearchPhase {
        init {
            require(query.isNotBlank())
        }
    }

    data class NothingIndexed(
        val query: String,
    ) : MeaningSearchPhase {
        init {
            require(query.isNotBlank())
        }
    }

    data class EngineUnavailable(
        val reason: String,
    ) : MeaningSearchPhase {
        init {
            require(reason.isNotBlank())
        }
    }

    data object SearchCouldNotFinish : MeaningSearchPhase
}

@HiltViewModel
class MeaningSearchViewModel(
    private val searchByMeaning: suspend (String) -> MeaningSearchOutcome,
    private val loadReadiness: suspend () -> MeaningSearchReadiness,
    private val openOriginal: suspend (MeaningSearchHit) -> MeaningOpenOriginalResult,
    private val launchOneNoteOriginal: (webUrl: String?, clientUrl: String?) -> Boolean,
    private val minSearchingVisibleMs: Long = DEFAULT_MIN_SEARCHING_VISIBLE_MS,
    private val monotonicMs: () -> Long = { System.nanoTime() / 1_000_000L },
) : ViewModel() {
    @Inject
    constructor(
        searchAssetMemoriesByMeaning: SearchAssetMemoriesByMeaning,
        loadMeaningSearchReadiness: LoadMeaningSearchReadiness,
        openMeaningSearchOriginal: OpenMeaningSearchOriginal,
        externalUrlLauncher: ExternalUrlLauncher,
    ) : this(
        searchByMeaning = { query -> searchAssetMemoriesByMeaning(query) },
        loadReadiness = { loadMeaningSearchReadiness() },
        openOriginal = { hit -> openMeaningSearchOriginal(hit) },
        launchOneNoteOriginal = { web, client ->
            externalUrlLauncher.launchOneNoteOriginal(web, client)
        },
    )

    private val mutableUiState = MutableStateFlow(MeaningSearchUiState())
    val uiState: StateFlow<MeaningSearchUiState> = mutableUiState.asStateFlow()

    private val searchGeneration = AtomicInteger(0)
    private val openGeneration = AtomicInteger(0)
    private var searchJob: Job? = null

    fun onScreenVisible() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(readiness = MeaningSearchReadinessUi.Loading) }
            val readiness = try {
                loadReadiness()
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(readiness = MeaningSearchReadinessUi.CouldNotLoad)
                }
                return@launch
            }
            mutableUiState.update { state ->
                state.copy(
                    readiness = when (readiness) {
                        is MeaningSearchReadiness.EngineUnavailable ->
                            MeaningSearchReadinessUi.EngineUnavailable(readiness.reason)
                        is MeaningSearchReadiness.Ready ->
                            MeaningSearchReadinessUi.Ready(readiness)
                    },
                )
            }
        }
    }

    fun onQueryChanged(value: String) {
        mutableUiState.update { state ->
            state.copy(
                query = value,
                phase = when (state.phase) {
                    is MeaningSearchPhase.Results,
                    is MeaningSearchPhase.NoMatches,
                    is MeaningSearchPhase.NothingIndexed,
                    is MeaningSearchPhase.EngineUnavailable,
                    MeaningSearchPhase.EmptyQuery,
                    MeaningSearchPhase.SearchCouldNotFinish,
                    -> MeaningSearchPhase.Idle
                    else -> state.phase
                },
            )
        }
    }

    fun onQueryCleared() {
        if (!mutableUiState.value.canClearQuery) return
        mutableUiState.update {
            it.copy(query = "", phase = MeaningSearchPhase.Idle)
        }
    }

    fun onSearch() {
        val query = mutableUiState.value.query
        if (query.isBlank()) {
            mutableUiState.update { it.copy(phase = MeaningSearchPhase.EmptyQuery) }
            return
        }
        if (mutableUiState.value.phase is MeaningSearchPhase.Searching) return
        if (mutableUiState.value.openFeedback is MeaningOpenFeedbackUi.Opening) return

        val generation = searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                phase = MeaningSearchPhase.Searching,
                openFeedback = MeaningOpenFeedbackUi.None,
                originalPreview = null,
            )
            val startedAtMs = monotonicMs()
            val outcome = try {
                searchByMeaning(query)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation != searchGeneration.get()) return@launch
                mutableUiState.update { state ->
                    if (generation != searchGeneration.get()) return@update state
                    if (state.phase !is MeaningSearchPhase.Searching) return@update state
                    state.copy(phase = MeaningSearchPhase.SearchCouldNotFinish)
                }
                return@launch
            }
            if (generation != searchGeneration.get()) return@launch

            val elapsedMs = monotonicMs() - startedAtMs
            val remainingMs = minSearchingVisibleMs - elapsedMs
            if (remainingMs > 0) delay(remainingMs)
            if (generation != searchGeneration.get()) return@launch

            val nextPhase = when (outcome) {
                MeaningSearchOutcome.BlankQuery -> MeaningSearchPhase.EmptyQuery
                is MeaningSearchOutcome.EngineUnavailable ->
                    MeaningSearchPhase.EngineUnavailable(outcome.reason)
                is MeaningSearchOutcome.NothingIndexed ->
                    MeaningSearchPhase.NothingIndexed(query = outcome.query)
                is MeaningSearchOutcome.Failed -> MeaningSearchPhase.SearchCouldNotFinish
                is MeaningSearchOutcome.Matches -> when {
                    outcome.hits.isEmpty() ->
                        MeaningSearchPhase.NoMatches(query = outcome.query)
                    else -> MeaningSearchPhase.Results(
                        query = outcome.query,
                        hits = outcome.hits,
                        limitReached = outcome.limitReached,
                    )
                }
            }
            mutableUiState.update { state ->
                if (generation != searchGeneration.get()) return@update state
                if (state.phase !is MeaningSearchPhase.Searching) return@update state
                state.copy(phase = nextPhase)
            }
        }
    }

    fun onSearchCancelled() {
        searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = null
        mutableUiState.update { state ->
            if (state.phase !is MeaningSearchPhase.Searching) return@update state
            state.copy(phase = MeaningSearchPhase.Idle)
        }
    }

    fun onOpenOriginal(hit: MeaningSearchHit) {
        if (mutableUiState.value.openFeedback is MeaningOpenFeedbackUi.Opening) return
        val generation = openGeneration.incrementAndGet()
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                openFeedback = MeaningOpenFeedbackUi.Opening,
                originalPreview = null,
            )
            val outcome = try {
                openOriginal(hit)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation != openGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    openFeedback = MeaningOpenFeedbackUi.CouldNotOpen,
                    originalPreview = null,
                )
                return@launch
            }
            if (generation != openGeneration.get()) return@launch
            mutableUiState.value = when (outcome) {
                is MeaningOpenOriginalResult.ScreenshotReady -> mutableUiState.value.copy(
                    openFeedback = MeaningOpenFeedbackUi.None,
                    originalPreview = MeaningOriginalPreviewUi.Screenshot(
                        ScreenshotOriginalPreviewUi(
                            screenshotLabel = outcome.label,
                            widthPx = outcome.widthPx,
                            heightPx = outcome.heightPx,
                            argb8888 = outcome.argb8888,
                        ),
                    ),
                )
                is MeaningOpenOriginalResult.PhotoReady -> mutableUiState.value.copy(
                    openFeedback = MeaningOpenFeedbackUi.None,
                    originalPreview = MeaningOriginalPreviewUi.Photo(
                        PhotoOriginalPreviewUi(
                            photoLabel = outcome.label,
                            widthPx = outcome.widthPx,
                            heightPx = outcome.heightPx,
                            argb8888 = outcome.argb8888,
                        ),
                    ),
                )
                is MeaningOpenOriginalResult.PdfReady -> mutableUiState.value.copy(
                    openFeedback = MeaningOpenFeedbackUi.None,
                    originalPreview = MeaningOriginalPreviewUi.Pdf(
                        PdfOriginalPreviewUi(
                            documentLabel = outcome.label,
                            pageNumber = outcome.pageNumber,
                            pageCount = outcome.pageCount,
                            widthPx = outcome.widthPx,
                            heightPx = outcome.heightPx,
                            argb8888 = outcome.argb8888,
                        ),
                    ),
                )
                is MeaningOpenOriginalResult.NoteReady -> {
                    val launched = try {
                        launchOneNoteOriginal(outcome.webUrl, outcome.clientUrl)
                    } catch (_: Exception) {
                        false
                    }
                    mutableUiState.value.copy(
                        openFeedback = if (launched) {
                            MeaningOpenFeedbackUi.None
                        } else {
                            MeaningOpenFeedbackUi.CouldNotOpen
                        },
                        originalPreview = null,
                    )
                }
                MeaningOpenOriginalResult.SourceUnavailable -> mutableUiState.value.copy(
                    openFeedback = MeaningOpenFeedbackUi.SourceUnavailable,
                    originalPreview = null,
                )
                MeaningOpenOriginalResult.CouldNotOpen -> mutableUiState.value.copy(
                    openFeedback = MeaningOpenFeedbackUi.CouldNotOpen,
                    originalPreview = null,
                )
            }
        }
    }

    fun onOpenFeedbackDismissed() {
        if (mutableUiState.value.openFeedback is MeaningOpenFeedbackUi.Opening) return
        mutableUiState.value = mutableUiState.value.copy(openFeedback = MeaningOpenFeedbackUi.None)
    }

    fun onOriginalPreviewClosed() {
        openGeneration.incrementAndGet()
        mutableUiState.value = mutableUiState.value.copy(
            originalPreview = null,
            openFeedback = MeaningOpenFeedbackUi.None,
        )
    }

    companion object {
        const val DEFAULT_MIN_SEARCHING_VISIBLE_MS = 350L
    }
}
