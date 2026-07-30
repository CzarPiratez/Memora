package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.images.LoadPersistedScreenshotOcrKeywordSearchReadiness
import com.memora.app.application.images.OpenPersistedScreenshotForViewing
import com.memora.app.application.images.ScreenshotOcrKeywordSearchHit
import com.memora.app.application.images.ScreenshotOcrKeywordSearchOutcome
import com.memora.app.application.images.ScreenshotOcrKeywordSearchReadiness
import com.memora.app.application.images.ScreenshotPreviewRenderResult
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
    val openFeedback: ScreenshotOpenFeedbackUi = ScreenshotOpenFeedbackUi.None,
    val originalPreview: ScreenshotOriginalPreviewUi? = null,
) {
    val canSubmitSearch: Boolean
        get() = phase !is ScreenshotOcrKeywordSearchPhase.Searching &&
            query.isNotBlank() &&
            openFeedback !is ScreenshotOpenFeedbackUi.Opening

    val canClearQuery: Boolean
        get() = query.isNotBlank() &&
            phase !is ScreenshotOcrKeywordSearchPhase.Searching &&
            openFeedback !is ScreenshotOpenFeedbackUi.Opening

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

sealed interface ScreenshotOpenFeedbackUi {
    data object None : ScreenshotOpenFeedbackUi

    data object Opening : ScreenshotOpenFeedbackUi

    data object SourceUnavailable : ScreenshotOpenFeedbackUi

    data object CouldNotOpen : ScreenshotOpenFeedbackUi
}

data class ScreenshotOriginalPreviewUi(
    val screenshotLabel: String,
    val widthPx: Int,
    val heightPx: Int,
    val argb8888: IntArray,
) {
    init {
        require(screenshotLabel.isNotBlank()) { "Preview UI needs a screenshot label." }
        require(widthPx > 0 && heightPx > 0)
        require(argb8888.size == widthPx * heightPx)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScreenshotOriginalPreviewUi) return false
        return screenshotLabel == other.screenshotLabel &&
            widthPx == other.widthPx &&
            heightPx == other.heightPx &&
            argb8888.contentEquals(other.argb8888)
    }

    override fun hashCode(): Int {
        var result = screenshotLabel.hashCode()
        result = 31 * result + widthPx
        result = 31 * result + heightPx
        result = 31 * result + argb8888.contentHashCode()
        return result
    }
}

@HiltViewModel
class ScreenshotOcrKeywordSearchViewModel(
    private val searchPersistedScreenshotOcrText: suspend (String) -> ScreenshotOcrKeywordSearchOutcome,
    private val loadReadiness: suspend () -> ScreenshotOcrKeywordSearchReadiness,
    private val openPersistedScreenshotForViewing:
        suspend (ScreenshotOcrKeywordSearchHit) -> ScreenshotPreviewRenderResult,
    private val minSearchingVisibleMs: Long = DEFAULT_MIN_SEARCHING_VISIBLE_MS,
    private val monotonicMs: () -> Long = { System.nanoTime() / 1_000_000L },
) : ViewModel() {
    @Inject
    constructor(
        searchPersistedScreenshotOcrText: SearchPersistedScreenshotOcrText,
        loadPersistedScreenshotOcrKeywordSearchReadiness:
            LoadPersistedScreenshotOcrKeywordSearchReadiness,
        openPersistedScreenshotForViewing: OpenPersistedScreenshotForViewing,
    ) : this(
        searchPersistedScreenshotOcrText = { rawQuery -> searchPersistedScreenshotOcrText(rawQuery) },
        loadReadiness = { loadPersistedScreenshotOcrKeywordSearchReadiness() },
        openPersistedScreenshotForViewing = { hit ->
            openPersistedScreenshotForViewing(
                sourceId = hit.sourceId,
                sourceAssetKey = hit.sourceAssetKey,
                screenshotLabel = hit.label,
            )
        },
    )

    private val mutableUiState = MutableStateFlow(ScreenshotOcrKeywordSearchUiState())
    val uiState: StateFlow<ScreenshotOcrKeywordSearchUiState> = mutableUiState.asStateFlow()

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
            phase = ScreenshotOcrKeywordSearchPhase.Idle,
            openFeedback = ScreenshotOpenFeedbackUi.None,
            originalPreview = null,
        )
    }

    fun onQueryCleared() {
        val current = mutableUiState.value
        if (!current.canClearQuery) return
        onQueryChanged("")
    }

    fun onSearch() {
        val query = mutableUiState.value.query
        if (mutableUiState.value.openFeedback is ScreenshotOpenFeedbackUi.Opening) return
        val generation = searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                phase = ScreenshotOcrKeywordSearchPhase.Searching,
                openFeedback = ScreenshotOpenFeedbackUi.None,
                originalPreview = null,
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
            state.copy(
                phase = ScreenshotOcrKeywordSearchPhase.Idle,
                openFeedback = ScreenshotOpenFeedbackUi.None,
                originalPreview = null,
            )
        }
    }

    fun onOpenOriginalScreenshot(hit: ScreenshotOcrKeywordSearchHit) {
        if (mutableUiState.value.openFeedback is ScreenshotOpenFeedbackUi.Opening) return
        val generation = openGeneration.incrementAndGet()
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                openFeedback = ScreenshotOpenFeedbackUi.Opening,
                originalPreview = null,
            )
            val outcome = try {
                openPersistedScreenshotForViewing(hit)
            } catch (_: Exception) {
                if (generation != openGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    openFeedback = ScreenshotOpenFeedbackUi.CouldNotOpen,
                    originalPreview = null,
                )
                return@launch
            }
            if (generation != openGeneration.get()) return@launch
            mutableUiState.value = when (outcome) {
                is ScreenshotPreviewRenderResult.Ready -> mutableUiState.value.copy(
                    openFeedback = ScreenshotOpenFeedbackUi.None,
                    originalPreview = ScreenshotOriginalPreviewUi(
                        screenshotLabel = outcome.screenshotLabel,
                        widthPx = outcome.widthPx,
                        heightPx = outcome.heightPx,
                        argb8888 = outcome.argb8888,
                    ),
                )
                ScreenshotPreviewRenderResult.SourceUnavailable -> mutableUiState.value.copy(
                    openFeedback = ScreenshotOpenFeedbackUi.SourceUnavailable,
                    originalPreview = null,
                )
                ScreenshotPreviewRenderResult.CouldNotOpen -> mutableUiState.value.copy(
                    openFeedback = ScreenshotOpenFeedbackUi.CouldNotOpen,
                    originalPreview = null,
                )
            }
        }
    }

    fun onOpenFeedbackDismissed() {
        if (mutableUiState.value.openFeedback is ScreenshotOpenFeedbackUi.Opening) return
        mutableUiState.value = mutableUiState.value.copy(openFeedback = ScreenshotOpenFeedbackUi.None)
    }

    fun onOriginalPreviewClosed() {
        openGeneration.incrementAndGet()
        mutableUiState.value = mutableUiState.value.copy(
            originalPreview = null,
            openFeedback = ScreenshotOpenFeedbackUi.None,
        )
    }

    fun onDerivedDataCleared() {
        searchGeneration.incrementAndGet()
        openGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = null
        mutableUiState.value = mutableUiState.value.copy(
            query = "",
            phase = ScreenshotOcrKeywordSearchPhase.Idle,
            readiness = ScreenshotOcrKeywordSearchReadinessUi.Ready(
                ScreenshotOcrKeywordSearchReadiness(screenshotCount = 0),
            ),
            openFeedback = ScreenshotOpenFeedbackUi.None,
            originalPreview = null,
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
