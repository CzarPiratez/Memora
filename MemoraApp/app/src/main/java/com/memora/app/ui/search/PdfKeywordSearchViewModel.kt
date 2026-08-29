package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.documents.LoadPersistedPdfKeywordSearchReadiness
import com.memora.app.application.documents.OpenPersistedPdfForViewing
import com.memora.app.application.documents.PdfKeywordSearchHit
import com.memora.app.application.documents.PdfKeywordSearchOutcome
import com.memora.app.application.documents.PdfKeywordSearchReadiness
import com.memora.app.application.documents.PdfMemoryEvidenceKeywordAdapter
import com.memora.app.application.documents.PdfPagePreviewRenderResult
import com.memora.app.application.memory.SearchMemoryEvidence
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

data class PdfKeywordSearchUiState(
    val query: String = "",
    val phase: PdfKeywordSearchPhase = PdfKeywordSearchPhase.Idle,
    val readiness: PdfKeywordSearchReadinessUi = PdfKeywordSearchReadinessUi.Loading,
    val openFeedback: PdfOpenFeedbackUi = PdfOpenFeedbackUi.None,
    val originalPreview: PdfOriginalPreviewUi? = null,
) {
    /** Shared gate for the Search button and keyboard Search action. */
    val canSubmitSearch: Boolean
        get() = phase !is PdfKeywordSearchPhase.Searching &&
            query.isNotBlank() &&
            openFeedback !is PdfOpenFeedbackUi.Opening

    /** Clear is available when there is typed text and no in-flight search/open. */
    val canClearQuery: Boolean
        get() = query.isNotBlank() &&
            phase !is PdfKeywordSearchPhase.Searching &&
            openFeedback !is PdfOpenFeedbackUi.Opening

    /** Cancel is available only while a search is in progress. */
    val canCancelSearch: Boolean
        get() = phase is PdfKeywordSearchPhase.Searching
}

sealed interface PdfKeywordSearchReadinessUi {
    data object Loading : PdfKeywordSearchReadinessUi

    data object CouldNotLoad : PdfKeywordSearchReadinessUi

    data class Ready(
        val snapshot: PdfKeywordSearchReadiness,
    ) : PdfKeywordSearchReadinessUi
}

sealed interface PdfKeywordSearchPhase {
    data object Idle : PdfKeywordSearchPhase

    data object EmptyQuery : PdfKeywordSearchPhase

    data object Searching : PdfKeywordSearchPhase

    data class Results(
        val query: String,
        val hits: List<PdfKeywordSearchHit>,
        val limitReached: Boolean,
    ) : PdfKeywordSearchPhase {
        init {
            require(query.isNotBlank()) { "Results need the search query for Explain Mode." }
            require(hits.isNotEmpty()) { "Results need at least one hit." }
        }
    }

    /** No listed hits for the submitted [query]. */
    data class NoMatches(
        val query: String,
    ) : PdfKeywordSearchPhase {
        init {
            require(query.isNotBlank()) { "No-matches needs the submitted search query." }
        }
    }

    /** Search ran, but no READY PDF Memory evidence exists to search. */
    data class NothingSavedToSearch(
        val query: String,
    ) : PdfKeywordSearchPhase {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved needs the submitted search query."
            }
        }
    }

    /** Search could not finish; never leave the UI spinning. */
    data object SearchCouldNotFinish : PdfKeywordSearchPhase
}

sealed interface PdfOpenFeedbackUi {
    data object None : PdfOpenFeedbackUi

    data object Opening : PdfOpenFeedbackUi

    data object SourceUnavailable : PdfOpenFeedbackUi

    data object CouldNotOpen : PdfOpenFeedbackUi
}

data class PdfOriginalPreviewUi(
    val documentLabel: String,
    val pageNumber: Int,
    val pageCount: Int,
    val widthPx: Int,
    val heightPx: Int,
    val argb8888: IntArray,
) {
    init {
        require(documentLabel.isNotBlank()) { "Preview UI needs a document label." }
        require(pageNumber > 0 && pageCount > 0 && pageNumber <= pageCount)
        require(widthPx > 0 && heightPx > 0)
        require(argb8888.size == widthPx * heightPx)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PdfOriginalPreviewUi) return false
        return documentLabel == other.documentLabel &&
            pageNumber == other.pageNumber &&
            pageCount == other.pageCount &&
            widthPx == other.widthPx &&
            heightPx == other.heightPx &&
            argb8888.contentEquals(other.argb8888)
    }

    override fun hashCode(): Int {
        var result = documentLabel.hashCode()
        result = 31 * result + pageNumber
        result = 31 * result + pageCount
        result = 31 * result + widthPx
        result = 31 * result + heightPx
        result = 31 * result + argb8888.contentHashCode()
        return result
    }
}

/**
 * PDF keyword Find — MIG-07 cutover to [SearchMemoryEvidence] (AssetType.PDF).
 *
 * L1 `SearchPersistedPdfPageText` retired; UI models preserved via adapter.
 */
@HiltViewModel
class PdfKeywordSearchViewModel(
    private val searchPdfKeyword: suspend (String) -> PdfKeywordSearchOutcome,
    private val loadReadiness: suspend () -> PdfKeywordSearchReadiness,
    private val openPersistedPdfForViewing: suspend (PdfKeywordSearchHit) -> PdfPagePreviewRenderResult,
    /**
     * Keeps Searching visible long enough to notice Cancel on small indexes.
     * Does not invent progress; slow searches are not delayed further.
     */
    private val minSearchingVisibleMs: Long = DEFAULT_MIN_SEARCHING_VISIBLE_MS,
    private val monotonicMs: () -> Long = { System.nanoTime() / 1_000_000L },
) : ViewModel() {
    @Inject
    constructor(
        searchMemoryEvidence: SearchMemoryEvidence,
        loadPersistedPdfKeywordSearchReadiness: LoadPersistedPdfKeywordSearchReadiness,
        openPersistedPdfForViewing: OpenPersistedPdfForViewing,
    ) : this(
        searchPdfKeyword = { rawQuery ->
            PdfMemoryEvidenceKeywordAdapter.toPdfOutcome(
                searchMemoryEvidence(
                    rawQuery = rawQuery,
                    assetType = AssetType.PDF,
                ),
            )
        },
        loadReadiness = { loadPersistedPdfKeywordSearchReadiness() },
        openPersistedPdfForViewing = { hit ->
            openPersistedPdfForViewing(
                sourceId = hit.sourceId,
                sourceAssetKey = hit.sourceAssetKey,
                pageNumber = hit.pageNumber,
                documentLabel = hit.label,
            )
        },
    )

    private val mutableUiState = MutableStateFlow(PdfKeywordSearchUiState())
    val uiState: StateFlow<PdfKeywordSearchUiState> = mutableUiState.asStateFlow()

    private val searchGeneration = AtomicInteger(0)
    private val readinessGeneration = AtomicInteger(0)
    private val openGeneration = AtomicInteger(0)
    private var searchJob: Job? = null

    init {
        refreshReadiness()
    }

    /** Call when Find saved PDF text becomes visible so counts stay current. */
    fun onScreenVisible() {
        refreshReadiness()
    }

    fun onQueryChanged(value: String) {
        val current = mutableUiState.value
        if (value == current.query) return

        // Field and phase must stay coherent: never leave Why/results for a prior query.
        searchGeneration.incrementAndGet()
        openGeneration.incrementAndGet()
        mutableUiState.value = current.copy(
            query = value,
            phase = PdfKeywordSearchPhase.Idle,
            openFeedback = PdfOpenFeedbackUi.None,
            originalPreview = null,
        )
    }

    /** Clears typed text and search/open UI; blocked while Searching or Opening. */
    fun onQueryCleared() {
        val current = mutableUiState.value
        if (!current.canClearQuery) return
        onQueryChanged("")
    }

    fun onSearch() {
        val query = mutableUiState.value.query
        val generation = searchGeneration.incrementAndGet()
        openGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                phase = PdfKeywordSearchPhase.Searching,
                openFeedback = PdfOpenFeedbackUi.None,
                originalPreview = null,
            )
            val startedAtMs = monotonicMs()
            val outcome = try {
                searchPdfKeyword(query)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation != searchGeneration.get()) return@launch
                mutableUiState.update { state ->
                    if (generation != searchGeneration.get()) return@update state
                    if (state.phase !is PdfKeywordSearchPhase.Searching) return@update state
                    state.copy(phase = PdfKeywordSearchPhase.SearchCouldNotFinish)
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
                PdfKeywordSearchOutcome.BlankQuery -> PdfKeywordSearchPhase.EmptyQuery
                is PdfKeywordSearchOutcome.NothingSavedToSearch ->
                    PdfKeywordSearchPhase.NothingSavedToSearch(query = outcome.query)
                is PdfKeywordSearchOutcome.Matches -> if (outcome.hits.isEmpty()) {
                    PdfKeywordSearchPhase.NoMatches(query = outcome.query)
                } else {
                    PdfKeywordSearchPhase.Results(
                        query = outcome.query,
                        hits = outcome.hits,
                        limitReached = outcome.limitReached,
                    )
                }
            }
            // Cancel may have moved Idle already; never overwrite a non-Searching phase.
            mutableUiState.update { state ->
                if (generation != searchGeneration.get()) return@update state
                if (state.phase !is PdfKeywordSearchPhase.Searching) return@update state
                state.copy(phase = nextPhase)
            }
        }
    }

    /**
     * Stops an in-flight search and returns to Idle with the typed query kept
     * so the user can edit or search again. Late completions are ignored.
     */
    fun onSearchCancelled() {
        searchGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = null
        mutableUiState.update { state ->
            if (state.phase !is PdfKeywordSearchPhase.Searching) return@update state
            state.copy(
                phase = PdfKeywordSearchPhase.Idle,
                openFeedback = PdfOpenFeedbackUi.None,
                originalPreview = null,
            )
        }
    }

    companion object {
        /** Long enough to notice and tap Cancel on tiny indexes; not a progress claim. */
        const val DEFAULT_MIN_SEARCHING_VISIBLE_MS = 700L
    }

    fun onOpenOriginalPdf(hit: PdfKeywordSearchHit) {
        if (mutableUiState.value.openFeedback is PdfOpenFeedbackUi.Opening) return
        val generation = openGeneration.incrementAndGet()
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                openFeedback = PdfOpenFeedbackUi.Opening,
                originalPreview = null,
            )
            val outcome = try {
                openPersistedPdfForViewing(hit)
            } catch (_: Exception) {
                if (generation != openGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    openFeedback = PdfOpenFeedbackUi.CouldNotOpen,
                    originalPreview = null,
                )
                return@launch
            }
            if (generation != openGeneration.get()) return@launch
            mutableUiState.value = when (outcome) {
                is PdfPagePreviewRenderResult.Ready -> mutableUiState.value.copy(
                    openFeedback = PdfOpenFeedbackUi.None,
                    originalPreview = PdfOriginalPreviewUi(
                        documentLabel = outcome.documentLabel,
                        pageNumber = outcome.pageNumber,
                        pageCount = outcome.pageCount,
                        widthPx = outcome.widthPx,
                        heightPx = outcome.heightPx,
                        argb8888 = outcome.argb8888,
                    ),
                )
                PdfPagePreviewRenderResult.SourceUnavailable -> mutableUiState.value.copy(
                    openFeedback = PdfOpenFeedbackUi.SourceUnavailable,
                    originalPreview = null,
                )
                PdfPagePreviewRenderResult.CouldNotOpen -> mutableUiState.value.copy(
                    openFeedback = PdfOpenFeedbackUi.CouldNotOpen,
                    originalPreview = null,
                )
            }
        }
    }

    fun onOpenFeedbackDismissed() {
        if (mutableUiState.value.openFeedback is PdfOpenFeedbackUi.Opening) return
        mutableUiState.value = mutableUiState.value.copy(openFeedback = PdfOpenFeedbackUi.None)
    }

    fun onOriginalPreviewClosed() {
        openGeneration.incrementAndGet()
        mutableUiState.value = mutableUiState.value.copy(
            originalPreview = null,
            openFeedback = PdfOpenFeedbackUi.None,
        )
    }

    /**
     * Drops Results/Why and typed query after user-confirmed index clear so
     * Explain Mode cannot cite excerpts that no longer exist, and the field
     * does not keep a phrase that no longer matches a live corpus.
     */
    fun onDerivedDataCleared() {
        searchGeneration.incrementAndGet()
        openGeneration.incrementAndGet()
        searchJob?.cancel()
        searchJob = null
        mutableUiState.value = mutableUiState.value.copy(
            query = "",
            phase = PdfKeywordSearchPhase.Idle,
            readiness = PdfKeywordSearchReadinessUi.Ready(
                PdfKeywordSearchReadiness(pageCount = 0, documentCount = 0),
            ),
            openFeedback = PdfOpenFeedbackUi.None,
            originalPreview = null,
        )
        refreshReadiness()
    }

    private fun refreshReadiness() {
        val generation = readinessGeneration.incrementAndGet()
        mutableUiState.value = mutableUiState.value.copy(
            readiness = PdfKeywordSearchReadinessUi.Loading,
        )
        viewModelScope.launch {
            val snapshot = try {
                loadReadiness()
            } catch (_: Exception) {
                if (generation != readinessGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    readiness = PdfKeywordSearchReadinessUi.CouldNotLoad,
                )
                return@launch
            }
            if (generation != readinessGeneration.get()) return@launch
            mutableUiState.value = mutableUiState.value.copy(
                readiness = PdfKeywordSearchReadinessUi.Ready(snapshot),
            )
        }
    }
}
