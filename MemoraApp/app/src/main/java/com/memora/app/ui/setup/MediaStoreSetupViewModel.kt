package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.discovery.MediaStoreImageIndexer
import com.memora.app.application.discovery.MediaStoreIndexingOutcome
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

/** A user-safe summary of a single explicit indexing request. */
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
 */
@HiltViewModel
class MediaStoreSetupViewModel @Inject constructor(
    private val indexMediaStoreImages: MediaStoreImageIndexer,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MediaStoreSetupUiState())

    val uiState: StateFlow<MediaStoreSetupUiState> = mutableUiState.asStateFlow()

    fun onPhotoPermissionResult(isGranted: Boolean) {
        mutableUiState.value = if (isGranted) {
            MediaStoreSetupUiState(photoAccess = PhotoAccessState.GRANTED)
        } else {
            MediaStoreSetupUiState(photoAccess = PhotoAccessState.REQUIRED)
        }
    }

    fun onDerivedDataCleared() {
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
        viewModelScope.launch {
            mutableUiState.value = indexMediaStoreImages().toUiState(current.photoAccess)
        }
    }

    private fun MediaStoreIndexingOutcome.toUiState(
        priorPhotoAccess: PhotoAccessState,
    ): MediaStoreSetupUiState = when (this) {
        is MediaStoreIndexingOutcome.Indexed -> MediaStoreSetupUiState(
            photoAccess = priorPhotoAccess,
            indexing = MediaStoreIndexingState.COMPLETED(
                discoveredAssetCount = discoveredAssetCount,
                hasMore = hasMore,
                accessScope = accessScope,
            ),
        )

        MediaStoreIndexingOutcome.AccessRequired -> MediaStoreSetupUiState(
            photoAccess = PhotoAccessState.REQUIRED,
            indexing = MediaStoreIndexingState.ACCESS_REQUIRED,
        )

        MediaStoreIndexingOutcome.AccessRevoked -> MediaStoreSetupUiState(
            photoAccess = PhotoAccessState.REQUIRED,
            indexing = MediaStoreIndexingState.ACCESS_REVOKED,
        )

        is MediaStoreIndexingOutcome.Failed -> MediaStoreSetupUiState(
            photoAccess = priorPhotoAccess,
            indexing = MediaStoreIndexingState.FAILED(failure.message),
        )
    }
}
