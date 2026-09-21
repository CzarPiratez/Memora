package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.asset.LoadSourceAvailability
import com.memora.app.application.asset.RecordOpenSourceAvailability
import com.memora.app.application.images.LoadPersistedScreenshotOcrKeywordSearchReadiness
import com.memora.app.application.images.OpenPersistedScreenshotForViewing
import com.memora.app.application.images.ScreenshotMemoryEvidenceKeywordAdapter
import com.memora.app.application.images.ScreenshotOcrKeywordSearchHit
import com.memora.app.application.images.ScreenshotOcrKeywordSearchOutcome
import com.memora.app.application.images.ScreenshotOcrKeywordSearchReadiness
import com.memora.app.application.images.ScreenshotPreviewRenderResult
import com.memora.app.application.find.FindThumbnailResult
import com.memora.app.application.memory.CanonicalRecall
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAvailabilityStatus
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
    val openTarget: FindOpenTarget? = null,
    val availability: Map<AssetIdentity, SourceAvailabilityStatus> = emptyMap(),
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

    fun openStateFor(target: FindOpenTarget): FindCardOpenState = when (openFeedback) {
        ScreenshotOpenFeedbackUi.None -> FindCardOpenState.IDLE
        ScreenshotOpenFeedbackUi.Opening ->
            if (openTarget == target) FindCardOpenState.OPENING else FindCardOpenState.IDLE
        ScreenshotOpenFeedbackUi.SourceUnavailable ->
            if (openTarget == target) {
                FindCardOpenState.SOURCE_UNAVAILABLE
            } else {
                FindCardOpenState.IDLE
            }
        ScreenshotOpenFeedbackUi.CouldNotOpen ->
            if (openTarget == target) {
                FindCardOpenState.COULD_NOT_OPEN
            } else {
                FindCardOpenState.IDLE
            }
    }

    fun availabilityFor(target: FindOpenTarget): SourceAvailabilityStatus =
        target.availabilityIn(availability)
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
    val sourceId: String,
    val sourceAssetKey: String,
    val screenshotLabel: String,
    val widthPx: Int,
    val heightPx: Int,
    val argb8888: IntArray,
) {
    init {
        require(sourceId.isNotBlank()) { "Preview UI needs a source id." }
        require(sourceAssetKey.isNotBlank()) { "Preview UI needs a source asset key." }
        require(screenshotLabel.isNotBlank()) { "Preview UI needs a screenshot label." }
        require(widthPx > 0 && heightPx > 0)
        require(argb8888.size == widthPx * heightPx)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScreenshotOriginalPreviewUi) return false
        return sourceId == other.sourceId &&
            sourceAssetKey == other.sourceAssetKey &&
            screenshotLabel == other.screenshotLabel &&
            widthPx == other.widthPx &&
            heightPx == other.heightPx &&
            argb8888.contentEquals(other.argb8888)
    }

    override fun hashCode(): Int {
        var result = sourceId.hashCode()
        result = 31 * result + sourceAssetKey.hashCode()
        result = 31 * result + screenshotLabel.hashCode()
        result = 31 * result + widthPx
        result = 31 * result + heightPx
        result = 31 * result + argb8888.contentHashCode()
        return result
    }
}

/**
 * Screenshot keyword Find via [CanonicalRecall] (KEYWORD path; AssetType.SCREENSHOT).
 *
 * L2 `SearchPersistedScreenshotOcrText` retired; UI models preserved via adapter.
 * Open-original remains source-identity based (no PDF page).
 */
@HiltViewModel
class ScreenshotOcrKeywordSearchViewModel(
    private val searchScreenshotKeyword: suspend (String) -> ScreenshotOcrKeywordSearchOutcome,
    private val loadReadiness: suspend () -> ScreenshotOcrKeywordSearchReadiness,
    private val openPersistedScreenshotForViewing:
        suspend (ScreenshotOcrKeywordSearchHit) -> ScreenshotPreviewRenderResult,
    private val minSearchingVisibleMs: Long = DEFAULT_MIN_SEARCHING_VISIBLE_MS,
    private val monotonicMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val loadAvailability: suspend (Collection<AssetIdentity>) ->
        Map<AssetIdentity, SourceAvailabilityStatus> = { emptyMap() },
    private val recordReachable: suspend (String, String) -> Unit = { _, _ -> },
    private val recordUnreachable: suspend (String, String) -> Unit = { _, _ -> },
) : ViewModel() {
    @Inject
    constructor(
        canonicalRecall: CanonicalRecall,
        loadPersistedScreenshotOcrKeywordSearchReadiness:
            LoadPersistedScreenshotOcrKeywordSearchReadiness,
        openPersistedScreenshotForViewing: OpenPersistedScreenshotForViewing,
        loadSourceAvailability: LoadSourceAvailability,
        recordOpenSourceAvailability: RecordOpenSourceAvailability,
    ) : this(
        searchScreenshotKeyword = { rawQuery ->
            ScreenshotMemoryEvidenceKeywordAdapter.toScreenshotOutcome(
                canonicalRecall(
                    rawQuery = rawQuery,
                    assetType = AssetType.SCREENSHOT,
                ),
            )
        },
        loadReadiness = { loadPersistedScreenshotOcrKeywordSearchReadiness() },
        openPersistedScreenshotForViewing = { hit ->
            openPersistedScreenshotForViewing(
                sourceId = hit.sourceId,
                sourceAssetKey = hit.sourceAssetKey,
                screenshotLabel = hit.label,
            )
        },
        loadAvailability = { loadSourceAvailability(it) },
        recordReachable = { id, key -> recordOpenSourceAvailability.reachable(id, key) },
        recordUnreachable = { id, key -> recordOpenSourceAvailability.unreachable(id, key) },
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
                searchScreenshotKeyword(query)
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
            val availability = when (val phase = nextPhase) {
                is ScreenshotOcrKeywordSearchPhase.Results -> loadAvailability(
                    phase.hits.map { it.recall.openTarget().asIdentity() },
                )
                else -> emptyMap()
            }
            mutableUiState.update { state ->
                if (generation != searchGeneration.get()) return@update state
                if (state.phase !is ScreenshotOcrKeywordSearchPhase.Searching) return@update state
                state.copy(phase = nextPhase, availability = availability)
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
        val target = hit.recall.openTarget()
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                openFeedback = ScreenshotOpenFeedbackUi.Opening,
                openTarget = target,
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
            val identity = target.asIdentity()
            val currentAvailability = mutableUiState.value.availability
            mutableUiState.value = when (outcome) {
                is ScreenshotPreviewRenderResult.Ready -> {
                    recordReachable(hit.sourceId, hit.sourceAssetKey)
                    mutableUiState.value.copy(
                        openFeedback = ScreenshotOpenFeedbackUi.None,
                        availability = currentAvailability +
                            (identity to SourceAvailabilityStatus.REACHABLE),
                        originalPreview = ScreenshotOriginalPreviewUi(
                            sourceId = hit.sourceId,
                            sourceAssetKey = hit.sourceAssetKey,
                            screenshotLabel = outcome.screenshotLabel,
                            widthPx = outcome.widthPx,
                            heightPx = outcome.heightPx,
                            argb8888 = outcome.argb8888,
                        ),
                    )
                }
                ScreenshotPreviewRenderResult.SourceUnavailable -> {
                    recordUnreachable(hit.sourceId, hit.sourceAssetKey)
                    mutableUiState.value.copy(
                        openFeedback = ScreenshotOpenFeedbackUi.SourceUnavailable,
                        originalPreview = null,
                        availability = currentAvailability +
                            (identity to SourceAvailabilityStatus.UNREACHABLE),
                    )
                }
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

    fun onThumbnailLoaded(target: FindOpenTarget, result: FindThumbnailResult) {
        val update = result.availabilityUpdate(target, mutableUiState.value.availability) ?: return
        viewModelScope.launch {
            when (update.first) {
                SourceAvailabilityStatus.REACHABLE ->
                    recordReachable(target.sourceId, target.sourceAssetKey)
                SourceAvailabilityStatus.UNREACHABLE ->
                    recordUnreachable(target.sourceId, target.sourceAssetKey)
                SourceAvailabilityStatus.UNKNOWN -> return@launch
            }
            mutableUiState.update { state ->
                val next = result.availabilityUpdate(target, state.availability) ?: return@update state
                state.copy(availability = next.second)
            }
        }
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
