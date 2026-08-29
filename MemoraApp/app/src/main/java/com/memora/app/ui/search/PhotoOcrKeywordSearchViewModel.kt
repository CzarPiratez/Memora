package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.images.LoadPersistedPhotoOcrKeywordSearchReadiness
import com.memora.app.application.images.OpenPersistedPhotoForViewing
import com.memora.app.application.images.PhotoMemoryEvidenceKeywordAdapter
import com.memora.app.application.images.PhotoOcrKeywordSearchHit
import com.memora.app.application.images.PhotoOcrKeywordSearchOutcome
import com.memora.app.application.images.PhotoOcrKeywordSearchReadiness
import com.memora.app.application.images.PhotoPreviewRenderResult
import com.memora.app.application.memory.SearchMemoryEvidence
import com.memora.app.domain.asset.AssetType
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
) {
    val canSubmitSearch get() = query.isNotBlank() && phase !is PhotoOcrKeywordSearchPhase.Searching
    val canCancelSearch get() = phase is PhotoOcrKeywordSearchPhase.Searching
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
    val photoLabel: String,
    val widthPx: Int,
    val heightPx: Int,
    val argb8888: IntArray,
)

/**
 * Photo keyword Find — MIG-07 cutover to [SearchMemoryEvidence]
 * (AssetType.PHOTO).
 *
 * L3 `SearchPersistedPhotoOcrText` retired; UI models preserved via adapter.
 * Open-original remains source-identity based (no PDF page).
 */
@HiltViewModel
class PhotoOcrKeywordSearchViewModel(
    private val search: suspend (String) -> PhotoOcrKeywordSearchOutcome,
    private val loadReadiness: suspend () -> PhotoOcrKeywordSearchReadiness,
    private val openPhoto: suspend (PhotoOcrKeywordSearchHit) -> PhotoPreviewRenderResult,
) : ViewModel() {
    @Inject
    constructor(
        searchMemoryEvidence: SearchMemoryEvidence,
        readiness: LoadPersistedPhotoOcrKeywordSearchReadiness,
        opener: OpenPersistedPhotoForViewing,
    ) : this(
        search = { rawQuery ->
            PhotoMemoryEvidenceKeywordAdapter.toPhotoOutcome(
                searchMemoryEvidence(
                    rawQuery = rawQuery,
                    assetType = AssetType.PHOTO,
                ),
            )
        },
        loadReadiness = { readiness() },
        openPhoto = { opener(it.sourceId, it.sourceAssetKey, it.label) },
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
            mutableState.value = mutableState.value.copy(phase = phase)
        }
    }

    fun onSearchCancelled() {
        generation.incrementAndGet()
        searchJob?.cancel()
        mutableState.value = mutableState.value.copy(phase = PhotoOcrKeywordSearchPhase.Idle)
    }

    fun onOpenOriginalPhoto(hit: PhotoOcrKeywordSearchHit) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(openFeedback = PhotoOpenFeedbackUi.Opening)
            mutableState.value = when (val result = runCatching { openPhoto(hit) }.getOrNull()) {
                is PhotoPreviewRenderResult.Ready -> mutableState.value.copy(
                    openFeedback = PhotoOpenFeedbackUi.None,
                    originalPreview = PhotoOriginalPreviewUi(
                        result.photoLabel,
                        result.widthPx,
                        result.heightPx,
                        result.argb8888,
                    ),
                )
                PhotoPreviewRenderResult.SourceUnavailable -> mutableState.value.copy(
                    openFeedback = PhotoOpenFeedbackUi.SourceUnavailable,
                )
                else -> mutableState.value.copy(openFeedback = PhotoOpenFeedbackUi.CouldNotOpen)
            }
        }
    }

    fun onOpenFeedbackDismissed() {
        mutableState.value = mutableState.value.copy(openFeedback = PhotoOpenFeedbackUi.None)
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
