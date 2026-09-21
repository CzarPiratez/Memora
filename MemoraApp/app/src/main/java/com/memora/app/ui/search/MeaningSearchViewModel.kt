package com.memora.app.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.asset.LoadFindOpenAvailability
import com.memora.app.application.asset.RecordOpenSourceAvailability
import com.memora.app.application.find.FindThumbnailResult
import com.memora.app.application.intelligence.LoadMeaningSearchReadiness
import com.memora.app.application.intelligence.MeaningOpenOriginalResult
import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.application.intelligence.MeaningSearchReadiness
import com.memora.app.application.intelligence.OpenMeaningSearchOriginal
import com.memora.app.application.memory.CanonicalRecall
import com.memora.app.application.notes.ExternalUrlLauncher
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAvailabilityStatus
import com.memora.app.domain.intelligence.RecallPrecision
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
    val availability: Map<AssetIdentity, SourceAvailabilityStatus> = emptyMap(),
) {
    /**
     * An open in flight no longer locks the screen. Opening a note waits on
     * Microsoft Graph, and freezing the search box for that long because
     * someone tapped a result is not a state the person asked for. Starting a
     * search abandons the open instead.
     */
    val canSubmitSearch: Boolean
        get() = phase !is MeaningSearchPhase.Searching && query.isNotBlank()

    val canClearQuery: Boolean
        get() = query.isNotBlank() && phase !is MeaningSearchPhase.Searching

    val canCancelSearch: Boolean
        get() = phase is MeaningSearchPhase.Searching

    /** What [target]'s own card should show. Every other card stays idle. */
    fun openStateFor(target: FindOpenTarget): FindCardOpenState = when (openFeedback) {
        MeaningOpenFeedbackUi.None -> FindCardOpenState.IDLE
        is MeaningOpenFeedbackUi.Opening ->
            if (openFeedback.target == target) FindCardOpenState.OPENING else FindCardOpenState.IDLE
        is MeaningOpenFeedbackUi.SourceUnavailable ->
            if (openFeedback.target == target) {
                FindCardOpenState.SOURCE_UNAVAILABLE
            } else {
                FindCardOpenState.IDLE
            }
        is MeaningOpenFeedbackUi.CouldNotOpen ->
            if (openFeedback.target == target) {
                FindCardOpenState.COULD_NOT_OPEN
            } else {
                FindCardOpenState.IDLE
            }
    }

    fun availabilityFor(target: FindOpenTarget): SourceAvailabilityStatus =
        target.availabilityIn(availability)
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

/**
 * Open-original feedback, always attached to the card it came from.
 *
 * @see FindOpenTarget for why the screen-level flag was wrong.
 */
sealed interface MeaningOpenFeedbackUi {
    data object None : MeaningOpenFeedbackUi

    data class Opening(val target: FindOpenTarget) : MeaningOpenFeedbackUi

    data class SourceUnavailable(val target: FindOpenTarget) : MeaningOpenFeedbackUi

    data class CouldNotOpen(val target: FindOpenTarget) : MeaningOpenFeedbackUi
}

/** The card identity for a meaning hit. */
fun MeaningSearchHit.openTarget(): FindOpenTarget = FindOpenTarget(
    sourceId = sourceId.value,
    sourceAssetKey = sourceAssetKey.value,
)

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
        /** Which of the person's words this list's evidence carries (D-12). */
        val precision: RecallPrecision = RecallPrecision.Exact,
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
    private val openOriginal: suspend (MeaningSearchHit, String) -> MeaningOpenOriginalResult,
    private val launchOneNoteOriginal: (webUrl: String?, clientUrl: String?) -> Boolean,
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
        loadMeaningSearchReadiness: LoadMeaningSearchReadiness,
        openMeaningSearchOriginal: OpenMeaningSearchOriginal,
        externalUrlLauncher: ExternalUrlLauncher,
        loadSourceAvailability: LoadFindOpenAvailability,
        recordOpenSourceAvailability: RecordOpenSourceAvailability,
    ) : this(
        searchByMeaning = { query -> canonicalRecall.searchByMeaning(query) },
        loadReadiness = { loadMeaningSearchReadiness() },
        openOriginal = { hit, query -> openMeaningSearchOriginal(hit, query) },
        launchOneNoteOriginal = { web, client ->
            externalUrlLauncher.launchOneNoteOriginal(web, client)
        },
        loadAvailability = { identities -> loadSourceAvailability(identities) },
        recordReachable = { id, key -> recordOpenSourceAvailability.reachable(id, key) },
        recordUnreachable = { id, key -> recordOpenSourceAvailability.unreachable(id, key) },
    )

    private val mutableUiState = MutableStateFlow(MeaningSearchUiState())
    val uiState: StateFlow<MeaningSearchUiState> = mutableUiState.asStateFlow()

    private val searchGeneration = AtomicInteger(0)
    private val openGeneration = AtomicInteger(0)
    private var searchJob: Job? = null
    private var openJob: Job? = null

    fun onScreenVisible() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(readiness = MeaningSearchReadinessUi.Loading) }
            val readiness = try {
                loadReadiness()
            } catch (error: Exception) {
                Log.w(TAG, "meaning search readiness failed", error)
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

        // A new search replaces the list the open belonged to, so abandon it
        // rather than leaving a Graph call running against a card that is gone.
        openGeneration.incrementAndGet()
        openJob?.cancel()
        openJob = null

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
            } catch (error: Exception) {
                Log.w(TAG, "meaning search threw", error)
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
                is MeaningSearchOutcome.Failed -> {
                    Log.w(TAG, "meaning search failed: ${outcome.reason}")
                    MeaningSearchPhase.SearchCouldNotFinish
                }
                is MeaningSearchOutcome.Matches -> when {
                    outcome.hits.isEmpty() ->
                        MeaningSearchPhase.NoMatches(query = outcome.query)
                    else -> MeaningSearchPhase.Results(
                        query = outcome.query,
                        hits = outcome.hits,
                        limitReached = outcome.limitReached,
                        precision = outcome.precision,
                    )
                }
            }
            if (generation != searchGeneration.get()) return@launch

            val availability = when (val phase = nextPhase) {
                is MeaningSearchPhase.Results -> loadAvailability(
                    phase.hits.map { it.openTarget().asIdentity() },
                )
                else -> emptyMap()
            }
            if (generation != searchGeneration.get()) return@launch

            mutableUiState.update { state ->
                if (generation != searchGeneration.get()) return@update state
                if (state.phase !is MeaningSearchPhase.Searching) return@update state
                state.copy(phase = nextPhase, availability = availability)
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

    /**
     * A tap while another open is in flight supersedes it rather than being
     * swallowed. Ignoring the second tap reads as a dead button — a note open
     * can sit on Graph for seconds, and during that window the person has
     * clearly told us they want a different file.
     */
    fun onOpenOriginal(hit: MeaningSearchHit) {
        val target = hit.openTarget()
        val generation = openGeneration.incrementAndGet()
        openJob?.cancel()
        openJob = viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                openFeedback = MeaningOpenFeedbackUi.Opening(target),
                originalPreview = null,
            )
            val cue = when (val phase = mutableUiState.value.phase) {
                is MeaningSearchPhase.Results -> phase.query
                else -> mutableUiState.value.query
            }
            val outcome = try {
                openOriginal(hit, cue)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation != openGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    openFeedback = MeaningOpenFeedbackUi.CouldNotOpen(target),
                    originalPreview = null,
                )
                return@launch
            }
            if (generation != openGeneration.get()) return@launch
            val identity = target.asIdentity()
            val currentAvailability = mutableUiState.value.availability
            mutableUiState.value = when (outcome) {
                is MeaningOpenOriginalResult.ScreenshotReady -> {
                    recordReachable(hit.sourceId.value, hit.sourceAssetKey.value)
                    mutableUiState.value.copy(
                        openFeedback = MeaningOpenFeedbackUi.None,
                        availability = currentAvailability + (identity to SourceAvailabilityStatus.REACHABLE),
                        originalPreview = MeaningOriginalPreviewUi.Screenshot(
                            ScreenshotOriginalPreviewUi(
                                sourceId = hit.sourceId.value,
                                sourceAssetKey = hit.sourceAssetKey.value,
                                screenshotLabel = outcome.label,
                                widthPx = outcome.widthPx,
                                heightPx = outcome.heightPx,
                                argb8888 = outcome.argb8888,
                            ),
                        ),
                    )
                }
                is MeaningOpenOriginalResult.PhotoReady -> {
                    recordReachable(hit.sourceId.value, hit.sourceAssetKey.value)
                    mutableUiState.value.copy(
                        openFeedback = MeaningOpenFeedbackUi.None,
                        availability = currentAvailability + (identity to SourceAvailabilityStatus.REACHABLE),
                        originalPreview = MeaningOriginalPreviewUi.Photo(
                            PhotoOriginalPreviewUi(
                                sourceId = hit.sourceId.value,
                                sourceAssetKey = hit.sourceAssetKey.value,
                                photoLabel = outcome.label,
                                widthPx = outcome.widthPx,
                                heightPx = outcome.heightPx,
                                argb8888 = outcome.argb8888,
                            ),
                        ),
                    )
                }
                is MeaningOpenOriginalResult.PdfReady -> {
                    recordReachable(hit.sourceId.value, hit.sourceAssetKey.value)
                    mutableUiState.value.copy(
                        openFeedback = MeaningOpenFeedbackUi.None,
                        availability = currentAvailability + (identity to SourceAvailabilityStatus.REACHABLE),
                        originalPreview = MeaningOriginalPreviewUi.Pdf(
                            PdfOriginalPreviewUi(
                                sourceId = hit.sourceId.value,
                                sourceAssetKey = hit.sourceAssetKey.value,
                                documentLabel = outcome.label,
                                pageNumber = outcome.pageNumber,
                                pageCount = outcome.pageCount,
                                widthPx = outcome.widthPx,
                                heightPx = outcome.heightPx,
                                argb8888 = outcome.argb8888,
                            ),
                        ),
                    )
                }
                is MeaningOpenOriginalResult.NoteReady -> {
                    val launched = try {
                        launchOneNoteOriginal(outcome.webUrl, outcome.clientUrl)
                    } catch (_: Exception) {
                        false
                    }
                    if (launched) {
                        recordReachable(hit.sourceId.value, hit.sourceAssetKey.value)
                    }
                    mutableUiState.value.copy(
                        openFeedback = if (launched) {
                            MeaningOpenFeedbackUi.None
                        } else {
                            MeaningOpenFeedbackUi.CouldNotOpen(target)
                        },
                        originalPreview = null,
                        availability = if (launched) {
                            currentAvailability + (identity to SourceAvailabilityStatus.REACHABLE)
                        } else {
                            currentAvailability
                        },
                    )
                }
                MeaningOpenOriginalResult.SourceUnavailable -> {
                    recordUnreachable(hit.sourceId.value, hit.sourceAssetKey.value)
                    mutableUiState.value.copy(
                        openFeedback = MeaningOpenFeedbackUi.SourceUnavailable(target),
                        originalPreview = null,
                        availability = currentAvailability + (identity to SourceAvailabilityStatus.UNREACHABLE),
                    )
                }
                MeaningOpenOriginalResult.CouldNotOpen -> mutableUiState.value.copy(
                    openFeedback = MeaningOpenFeedbackUi.CouldNotOpen(target),
                    originalPreview = null,
                )
            }
        }
    }

    fun onOpenFeedbackDismissed() {
        if (mutableUiState.value.openFeedback is MeaningOpenFeedbackUi.Opening) return
        mutableUiState.value = mutableUiState.value.copy(openFeedback = MeaningOpenFeedbackUi.None)
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
        openJob?.cancel()
        openJob = null
        mutableUiState.value = mutableUiState.value.copy(
            originalPreview = null,
            openFeedback = MeaningOpenFeedbackUi.None,
        )
    }

    companion object {
        private const val TAG = "MeaningSearchViewModel"

        const val DEFAULT_MIN_SEARCHING_VISIBLE_MS = 350L
    }
}
