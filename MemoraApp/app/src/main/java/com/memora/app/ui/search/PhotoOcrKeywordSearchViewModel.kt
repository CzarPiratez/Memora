package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.asset.LoadSourceAvailability
import com.memora.app.application.asset.RecordOpenSourceAvailability
import com.memora.app.application.images.LoadPersistedPhotoOcrKeywordSearchReadiness
import com.memora.app.application.images.OpenPersistedPhotoForViewing
import com.memora.app.application.images.PhotoMemoryEvidenceKeywordAdapter
import com.memora.app.application.images.PhotoOcrKeywordSearchHit
import com.memora.app.application.images.PhotoOcrKeywordSearchOutcome
import com.memora.app.application.images.PhotoOcrKeywordSearchReadiness
import com.memora.app.application.images.PhotoPreviewRenderResult
import com.memora.app.application.find.FindThumbnailResult
import com.memora.app.application.memory.CanonicalRecall
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAvailabilityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PhotoOcrKeywordSearchUiState(
    val query: String = "",
    val phase: PhotoOcrKeywordSearchPhase = PhotoOcrKeywordSearchPhase.Idle,
    val readiness: PhotoOcrKeywordSearchReadinessUi = PhotoOcrKeywordSearchReadinessUi.Loading,
    val openFeedback: PhotoOpenFeedbackUi = PhotoOpenFeedbackUi.None,
    val originalPreview: PhotoOriginalPreviewUi? = null,
    val openTarget: FindOpenTarget? = null,
    val availability: Map<AssetIdentity, SourceAvailabilityStatus> = emptyMap(),
) {
    val canSubmitSearch get() = query.isNotBlank() && phase !is PhotoOcrKeywordSearchPhase.Searching
    val canCancelSearch get() = phase is PhotoOcrKeywordSearchPhase.Searching

    fun openStateFor(target: FindOpenTarget): FindCardOpenState = when (openFeedback) {
        PhotoOpenFeedbackUi.None -> FindCardOpenState.IDLE
        PhotoOpenFeedbackUi.Opening ->
            if (openTarget == target) FindCardOpenState.OPENING else FindCardOpenState.IDLE
        PhotoOpenFeedbackUi.SourceUnavailable ->
            if (openTarget == target) FindCardOpenState.SOURCE_UNAVAILABLE else FindCardOpenState.IDLE
        PhotoOpenFeedbackUi.CouldNotOpen ->
            if (openTarget == target) FindCardOpenState.COULD_NOT_OPEN else FindCardOpenState.IDLE
    }

    fun availabilityFor(target: FindOpenTarget): SourceAvailabilityStatus =
        target.availabilityIn(availability)
}

sealed interface PhotoOcrKeywordSearchReadinessUi {
    data object Loading : PhotoOcrKeywordSearchReadinessUi
    data object CouldNotLoad : PhotoOcrKeywordSearchReadinessUi
    data class Ready(val snapshot: PhotoOcrKeywordSearchReadiness) :
        PhotoOcrKeywordSearchReadinessUi
}

sealed interface PhotoOcrKeywordSearchPhase {
    data object Idle : PhotoOcrKeywordSearchPhase
    data object Searching : PhotoOcrKeywordSearchPhase
    data class Results(
        val query: String,
        val hits: List<PhotoOcrKeywordSearchHit>,
        val limitReached: Boolean,
    ) : PhotoOcrKeywordSearchPhase
    data class NoMatches(val query: String) : PhotoOcrKeywordSearchPhase
    data class NothingSavedToSearch(val query: String) : PhotoOcrKeywordSearchPhase
    data object SearchCouldNotFinish : PhotoOcrKeywordSearchPhase
}

sealed interface PhotoOpenFeedbackUi {
    data object None : PhotoOpenFeedbackUi
    data object Opening : PhotoOpenFeedbackUi
    data object SourceUnavailable : PhotoOpenFeedbackUi
    data object CouldNotOpen : PhotoOpenFeedbackUi
}

data class PhotoOriginalPreviewUi(
    val sourceId: String,
    val sourceAssetKey: String,
    val photoLabel: String,
    val widthPx: Int,
    val heightPx: Int,
    val argb8888: IntArray,
) {
    init {
        require(sourceId.isNotBlank()) { "Preview UI needs a source id." }
        require(sourceAssetKey.isNotBlank()) { "Preview UI needs a source asset key." }
        require(photoLabel.isNotBlank()) { "Preview UI needs a photo label." }
        require(widthPx > 0 && heightPx > 0)
        require(argb8888.size == widthPx * heightPx)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhotoOriginalPreviewUi) return false
        return sourceId == other.sourceId &&
            sourceAssetKey == other.sourceAssetKey &&
            photoLabel == other.photoLabel &&
            widthPx == other.widthPx &&
            heightPx == other.heightPx &&
            argb8888.contentEquals(other.argb8888)
    }

    override fun hashCode(): Int {
        var result = sourceId.hashCode()
        result = 31 * result + sourceAssetKey.hashCode()
        result = 31 * result + photoLabel.hashCode()
        result = 31 * result + widthPx
        result = 31 * result + heightPx
        result = 31 * result + argb8888.contentHashCode()
        return result
    }
}

/**
 * Photo keyword Find via [CanonicalRecall] (KEYWORD path; AssetType.PHOTO).
 *
 * L3 `SearchPersistedPhotoOcrText` retired; UI models preserved via adapter.
 * Open-original remains source-identity based (no PDF page).
 */
@HiltViewModel
class PhotoOcrKeywordSearchViewModel(
    private val search: suspend (String) -> PhotoOcrKeywordSearchOutcome,
    private val loadReadiness: suspend () -> PhotoOcrKeywordSearchReadiness,
    private val openPhoto: suspend (PhotoOcrKeywordSearchHit) -> PhotoPreviewRenderResult,
    private val loadAvailability: suspend (Collection<AssetIdentity>) ->
        Map<AssetIdentity, SourceAvailabilityStatus> = { emptyMap() },
    private val recordReachable: suspend (String, String) -> Unit = { _, _ -> },
    private val recordUnreachable: suspend (String, String) -> Unit = { _, _ -> },
) : ViewModel() {
    @Inject
    constructor(
        canonicalRecall: CanonicalRecall,
        readiness: LoadPersistedPhotoOcrKeywordSearchReadiness,
        opener: OpenPersistedPhotoForViewing,
        loadSourceAvailability: LoadSourceAvailability,
        recordOpenSourceAvailability: RecordOpenSourceAvailability,
    ) : this(
        search = { rawQuery ->
            PhotoMemoryEvidenceKeywordAdapter.toPhotoOutcome(
                canonicalRecall(
                    rawQuery = rawQuery,
                    assetType = AssetType.PHOTO,
                ),
            )
        },
        loadReadiness = { readiness() },
        openPhoto = { opener(it.sourceId, it.sourceAssetKey, it.label) },
        loadAvailability = { loadSourceAvailability(it) },
        recordReachable = { id, key -> recordOpenSourceAvailability.reachable(id, key) },
        recordUnreachable = { id, key -> recordOpenSourceAvailability.unreachable(id, key) },
    )

    private val mutableState = MutableStateFlow(PhotoOcrKeywordSearchUiState())
    val uiState = mutableState.asStateFlow()
    private val generation = AtomicInteger()
    private var searchJob: Job? = null

    init {
        refreshReadiness()
    }

    fun onScreenVisible() = refreshReadiness()

    fun onQueryChanged(value: String) {
        generation.incrementAndGet()
        mutableState.value = mutableState.value.copy(
            query = value,
            phase = PhotoOcrKeywordSearchPhase.Idle,
            openFeedback = PhotoOpenFeedbackUi.None,
            originalPreview = null,
        )
    }

    fun onQueryCleared() = onQueryChanged("")

    fun onSearch() {
        val query = mutableState.value.query
        if (query.isBlank()) return
        val current = generation.incrementAndGet()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(phase = PhotoOcrKeywordSearchPhase.Searching)
            val outcome = runCatching { search(query) }.getOrElse {
                if (current == generation.get()) {
                    mutableState.value = mutableState.value.copy(
                        phase = PhotoOcrKeywordSearchPhase.SearchCouldNotFinish,
                    )
                }
                return@launch
            }
            if (current != generation.get()) return@launch
            val phase = when (outcome) {
                PhotoOcrKeywordSearchOutcome.BlankQuery -> PhotoOcrKeywordSearchPhase.Idle
                is PhotoOcrKeywordSearchOutcome.NothingSavedToSearch ->
                    PhotoOcrKeywordSearchPhase.NothingSavedToSearch(outcome.query)
                is PhotoOcrKeywordSearchOutcome.Matches -> if (outcome.hits.isEmpty()) {
                    PhotoOcrKeywordSearchPhase.NoMatches(outcome.query)
                } else {
                    PhotoOcrKeywordSearchPhase.Results(
                        outcome.query,
                        outcome.hits,
                        outcome.limitReached,
                    )
                }
            }
            if (current != generation.get()) return@launch
            val availability = when (val next = phase) {
                is PhotoOcrKeywordSearchPhase.Results -> loadAvailability(
                    next.hits.map { it.recall.openTarget().asIdentity() },
                )
                else -> emptyMap()
            }
            mutableState.value = mutableState.value.copy(phase = phase, availability = availability)
        }
    }

    fun onSearchCancelled() {
        generation.incrementAndGet()
        searchJob?.cancel()
        mutableState.value = mutableState.value.copy(phase = PhotoOcrKeywordSearchPhase.Idle)
    }

    fun onOpenOriginalPhoto(hit: PhotoOcrKeywordSearchHit) {
        val target = hit.recall.openTarget()
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                openFeedback = PhotoOpenFeedbackUi.Opening,
                openTarget = target,
            )
            val identity = target.asIdentity()
            mutableState.value = when (val result = runCatching { openPhoto(hit) }.getOrNull()) {
                is PhotoPreviewRenderResult.Ready -> {
                    recordReachable(hit.sourceId, hit.sourceAssetKey)
                    mutableState.value.copy(
                        openFeedback = PhotoOpenFeedbackUi.None,
                        availability = mutableState.value.availability +
                            (identity to SourceAvailabilityStatus.REACHABLE),
                        originalPreview = PhotoOriginalPreviewUi(
                            sourceId = hit.sourceId,
                            sourceAssetKey = hit.sourceAssetKey,
                            photoLabel = result.photoLabel,
                            widthPx = result.widthPx,
                            heightPx = result.heightPx,
                            argb8888 = result.argb8888,
                        ),
                    )
                }
                PhotoPreviewRenderResult.SourceUnavailable -> {
                    recordUnreachable(hit.sourceId, hit.sourceAssetKey)
                    mutableState.value.copy(
                        openFeedback = PhotoOpenFeedbackUi.SourceUnavailable,
                        availability = mutableState.value.availability +
                            (identity to SourceAvailabilityStatus.UNREACHABLE),
                    )
                }
                else -> mutableState.value.copy(openFeedback = PhotoOpenFeedbackUi.CouldNotOpen)
            }
        }
    }

    fun onOpenFeedbackDismissed() {
        mutableState.value = mutableState.value.copy(openFeedback = PhotoOpenFeedbackUi.None)
    }

    fun onThumbnailLoaded(target: FindOpenTarget, result: FindThumbnailResult) {
        val update = result.availabilityUpdate(target, mutableState.value.availability) ?: return
        viewModelScope.launch {
            when (update.first) {
                SourceAvailabilityStatus.REACHABLE ->
                    recordReachable(target.sourceId, target.sourceAssetKey)
                SourceAvailabilityStatus.UNREACHABLE ->
                    recordUnreachable(target.sourceId, target.sourceAssetKey)
                SourceAvailabilityStatus.UNKNOWN -> return@launch
            }
            val next = result.availabilityUpdate(target, mutableState.value.availability) ?: return@launch
            mutableState.value = mutableState.value.copy(availability = next.second)
        }
    }

    fun onOriginalPreviewClosed() {
        mutableState.value = mutableState.value.copy(originalPreview = null)
    }

    fun onDerivedDataCleared() {
        generation.incrementAndGet()
        searchJob?.cancel()
        mutableState.value = PhotoOcrKeywordSearchUiState(
            readiness = PhotoOcrKeywordSearchReadinessUi.Ready(
                PhotoOcrKeywordSearchReadiness(0),
            ),
        )
    }

    private fun refreshReadiness() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                readiness = runCatching { loadReadiness() }
                    .fold(
                        onSuccess = { PhotoOcrKeywordSearchReadinessUi.Ready(it) },
                        onFailure = { PhotoOcrKeywordSearchReadinessUi.CouldNotLoad },
                    ),
            )
        }
    }
}
