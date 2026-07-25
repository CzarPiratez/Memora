package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import com.memora.app.work.DefaultMediaStoreDiscoveryWorkScheduler
import com.memora.app.work.MediaStoreDiscoveryWorkScheduler
import com.memora.app.work.MediaStoreDiscoveryWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Immutable state rendered by the future setup screen. */
data class MediaStoreSetupUiState(
    val photoAccess: PhotoAccessState = PhotoAccessState.REQUIRED,
    val indexing: MediaStoreIndexingState = MediaStoreIndexingState.NOT_STARTED,
)

/** Android permission facts reported by UI; the ViewModel never requests them itself. */
enum class PhotoAccessState {
    REQUIRED,
    GRANTED,
}

/** A user-safe summary of MediaStore discovery drain progress. */
sealed interface MediaStoreIndexingState {
    data object NOT_STARTED : MediaStoreIndexingState

    data object IN_PROGRESS : MediaStoreIndexingState

    data class COMPLETED(
        val discoveredAssetCount: Int,
        val hasMore: Boolean,
        val accessScope: ImageLibraryAccessScope,
    ) : MediaStoreIndexingState

    data object ACCESS_REQUIRED : MediaStoreIndexingState

    data object ACCESS_REVOKED : MediaStoreIndexingState

    data class FAILED(
        val message: String,
    ) : MediaStoreIndexingState
}

/**
 * Presentation boundary for the photo setup flow.
 *
 * Android permission requests remain in the Activity/Compose layer. Indexing begins
 * only when that layer reports granted access and the user explicitly requests it.
 * Discovery metadata only; does not open image bytes or schedule OCR.
 */
@HiltViewModel
class MediaStoreSetupViewModel @Inject constructor(
    private val discoveryWorkScheduler: MediaStoreDiscoveryWorkScheduler,
    private val assetRepository: AssetRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MediaStoreSetupUiState())
    private var workObservationJob: Job? = null

    val uiState: StateFlow<MediaStoreSetupUiState> = mutableUiState.asStateFlow()

    fun onPhotoPermissionResult(isGranted: Boolean) {
        workObservationJob?.cancel()
        mutableUiState.value = if (isGranted) {
            MediaStoreSetupUiState(photoAccess = PhotoAccessState.GRANTED)
        } else {
            MediaStoreSetupUiState(photoAccess = PhotoAccessState.REQUIRED)
        }
    }

    fun onDerivedDataCleared() {
        workObservationJob?.cancel()
        val photoAccess = mutableUiState.value.photoAccess
        mutableUiState.value = MediaStoreSetupUiState(photoAccess = photoAccess)
    }

    fun onIndexRequested() {
        val current = mutableUiState.value
        if (current.photoAccess != PhotoAccessState.GRANTED ||
            current.indexing == MediaStoreIndexingState.IN_PROGRESS
        ) {
            return
        }

        mutableUiState.value = current.copy(indexing = MediaStoreIndexingState.IN_PROGRESS)
        discoveryWorkScheduler.enqueueDrain()
        observeDiscoveryWork()
    }

    private fun observeDiscoveryWork() {
        workObservationJob?.cancel()
        workObservationJob = viewModelScope.launch {
            discoveryWorkScheduler.observeUniqueWork().collect { infos ->
                applyWorkInfos(infos)
            }
        }
    }

    private suspend fun applyWorkInfos(infos: List<WorkInfo>) {
        if (infos.isEmpty()) return
        if (mutableUiState.value.photoAccess != PhotoAccessState.GRANTED &&
            mutableUiState.value.indexing != MediaStoreIndexingState.IN_PROGRESS
        ) {
            return
        }

        val sourceId = DefaultMediaStoreDiscoveryWorkScheduler.SOURCE_ID
        val totalAssets = runCatching {
            assetRepository.countBySourceAndType(sourceId, AssetType.PHOTO) +
                assetRepository.countBySourceAndType(sourceId, AssetType.SCREENSHOT)
        }.getOrDefault(0)

        when {
            infos.any { info ->
                info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED
            } -> {
                mutableUiState.value = mutableUiState.value.copy(
                    indexing = MediaStoreIndexingState.IN_PROGRESS,
                )
            }

            infos.any { it.state == WorkInfo.State.FAILED } -> {
                val failed = infos.lastOrNull { it.state == WorkInfo.State.FAILED }
                val reason = failed?.outputData?.getString(MediaStoreDiscoveryWorker.KEY_FAILURE_REASON)
                if (reason == MediaStoreDiscoveryWorker.REASON_ACCESS_STOPPED) {
                    mutableUiState.value = MediaStoreSetupUiState(
                        photoAccess = PhotoAccessState.REQUIRED,
                        indexing = MediaStoreIndexingState.ACCESS_REVOKED,
                    )
                } else {
                    mutableUiState.value = mutableUiState.value.copy(
                        indexing = MediaStoreIndexingState.FAILED(
                            "Memora could not finish reading photo metadata. You can try again.",
                        ),
                    )
                }
            }

            infos.all { it.state.isFinished } -> {
                val lastSuccess = infos.lastOrNull { it.state == WorkInfo.State.SUCCEEDED }
                val hasMore = lastSuccess?.outputData?.getBoolean(
                    MediaStoreDiscoveryWorker.KEY_HAS_MORE,
                    false,
                ) == true
                val accessScope = lastSuccess?.outputData
                    ?.getString(MediaStoreDiscoveryWorker.KEY_ACCESS_SCOPE)
                    ?.let { runCatching { ImageLibraryAccessScope.valueOf(it) }.getOrNull() }
                    ?: ImageLibraryAccessScope.FULL_LIBRARY
                mutableUiState.value = MediaStoreSetupUiState(
                    photoAccess = PhotoAccessState.GRANTED,
                    indexing = MediaStoreIndexingState.COMPLETED(
                        discoveredAssetCount = totalAssets,
                        hasMore = hasMore,
                        accessScope = accessScope,
                    ),
                )
            }
        }
    }
}
