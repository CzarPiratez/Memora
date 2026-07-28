package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.memora.app.application.images.RunPendingImageExifExtract
import com.memora.app.application.images.RunPendingScreenshotOcrExtract
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import com.memora.app.domain.extraction.ImageExifExtractionPersistence
import com.memora.app.domain.extraction.ScreenshotOcrExtractionPersistence
import com.memora.app.work.DefaultMediaStoreDiscoveryWorkScheduler
import com.memora.app.work.MediaStoreDiscoveryWorkScheduler
import com.memora.app.work.MediaStoreDiscoveryWorker
import com.memora.app.work.MediaStoreImageExifExtractWorkScheduler
import com.memora.app.work.MediaStoreImageExifExtractWorker
import com.memora.app.work.MediaStoreScreenshotOcrExtractWorkScheduler
import com.memora.app.work.MediaStoreScreenshotOcrExtractWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Immutable state rendered by the photo setup screen. */
data class MediaStoreSetupUiState(
    val photoAccess: PhotoAccessState = PhotoAccessState.REQUIRED,
    val indexing: MediaStoreIndexingState = MediaStoreIndexingState.NOT_STARTED,
    val exifExtract: ImageExifExtractUiState = ImageExifExtractUiState.NotStarted,
    val screenshotCatalogueCount: Int = 0,
    val screenshotOcr: ScreenshotOcrExtractUiState = ScreenshotOcrExtractUiState.NotStarted,
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

/** User-safe progress for deterministic EXIF fact reading (not OCR / search). */
sealed interface ImageExifExtractUiState {
    data object NotStarted : ImageExifExtractUiState

    data object InProgress : ImageExifExtractUiState

    data class Completed(
        val extractedCount: Int,
        val catalogueCount: Int,
    ) : ImageExifExtractUiState

    data object AccessStopped : ImageExifExtractUiState

    data class Failed(
        val message: String,
    ) : ImageExifExtractUiState
}

/** User-safe progress for deterministic screenshot OCR (not keyword search / Memory). */
sealed interface ScreenshotOcrExtractUiState {
    data object NotStarted : ScreenshotOcrExtractUiState

    data object InProgress : ScreenshotOcrExtractUiState

    data class Completed(
        val extractedCount: Int,
        val screenshotCatalogueCount: Int,
    ) : ScreenshotOcrExtractUiState

    data object AccessStopped : ScreenshotOcrExtractUiState

    data class Failed(
        val message: String,
    ) : ScreenshotOcrExtractUiState
}

/**
 * Presentation boundary for the photo setup flow.
 *
 * Discovery catalogues metadata without opening bytes (ADR-009). EXIF and screenshot
 * OCR are separate explicit steps that may open permitted images read-only.
 */
@HiltViewModel
class MediaStoreSetupViewModel @Inject constructor(
    private val discoveryWorkScheduler: MediaStoreDiscoveryWorkScheduler,
    private val exifExtractWorkScheduler: MediaStoreImageExifExtractWorkScheduler,
    private val screenshotOcrWorkScheduler: MediaStoreScreenshotOcrExtractWorkScheduler,
    private val assetRepository: AssetRepository,
    private val imageExifPersistence: ImageExifExtractionPersistence,
    private val screenshotOcrPersistence: ScreenshotOcrExtractionPersistence,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MediaStoreSetupUiState())
    private var discoveryObservationJob: Job? = null
    private var exifObservationJob: Job? = null
    private var ocrObservationJob: Job? = null

    val uiState: StateFlow<MediaStoreSetupUiState> = mutableUiState.asStateFlow()

    fun onPhotoPermissionResult(isGranted: Boolean) {
        discoveryObservationJob?.cancel()
        exifObservationJob?.cancel()
        ocrObservationJob?.cancel()
        mutableUiState.value = if (isGranted) {
            MediaStoreSetupUiState(photoAccess = PhotoAccessState.GRANTED)
        } else {
            MediaStoreSetupUiState(photoAccess = PhotoAccessState.REQUIRED)
        }
    }

    fun onDerivedDataCleared() {
        discoveryObservationJob?.cancel()
        exifObservationJob?.cancel()
        ocrObservationJob?.cancel()
        val photoAccess = mutableUiState.value.photoAccess
        mutableUiState.value = MediaStoreSetupUiState(photoAccess = photoAccess)
    }

    fun onIndexRequested() {
        val current = mutableUiState.value
        if (current.photoAccess != PhotoAccessState.GRANTED ||
            current.indexing == MediaStoreIndexingState.IN_PROGRESS ||
            current.exifExtract is ImageExifExtractUiState.InProgress ||
            current.screenshotOcr is ScreenshotOcrExtractUiState.InProgress
        ) {
            return
        }

        mutableUiState.value = current.copy(
            indexing = MediaStoreIndexingState.IN_PROGRESS,
            exifExtract = ImageExifExtractUiState.NotStarted,
            screenshotCatalogueCount = 0,
            screenshotOcr = ScreenshotOcrExtractUiState.NotStarted,
        )
        discoveryWorkScheduler.enqueueDrain()
        observeDiscoveryWork()
    }

    fun onExifExtractRequested() {
        val current = mutableUiState.value
        val indexing = current.indexing
        if (current.photoAccess != PhotoAccessState.GRANTED ||
            indexing !is MediaStoreIndexingState.COMPLETED ||
            indexing.hasMore ||
            indexing.discoveredAssetCount <= 0 ||
            current.exifExtract is ImageExifExtractUiState.InProgress ||
            current.screenshotOcr is ScreenshotOcrExtractUiState.InProgress
        ) {
            return
        }

        mutableUiState.value = current.copy(
            exifExtract = ImageExifExtractUiState.InProgress,
            screenshotOcr = ScreenshotOcrExtractUiState.NotStarted,
        )
        val sourceId = DefaultMediaStoreDiscoveryWorkScheduler.SOURCE_ID
        exifExtractWorkScheduler.enqueueDrain(sourceId)
        observeExifExtractWork()
    }

    fun onScreenshotOcrExtractRequested() {
        val current = mutableUiState.value
        if (current.photoAccess != PhotoAccessState.GRANTED ||
            current.exifExtract !is ImageExifExtractUiState.Completed ||
            current.screenshotCatalogueCount <= 0 ||
            current.screenshotOcr is ScreenshotOcrExtractUiState.InProgress
        ) {
            return
        }

        mutableUiState.value = current.copy(screenshotOcr = ScreenshotOcrExtractUiState.InProgress)
        val sourceId = DefaultMediaStoreDiscoveryWorkScheduler.SOURCE_ID
        screenshotOcrWorkScheduler.enqueueDrain(sourceId)
        observeScreenshotOcrWork()
    }

    private fun observeDiscoveryWork() {
        discoveryObservationJob?.cancel()
        discoveryObservationJob = viewModelScope.launch {
            discoveryWorkScheduler.observeUniqueWork().collect { infos ->
                applyDiscoveryWorkInfos(infos)
            }
        }
    }

    private fun observeExifExtractWork() {
        exifObservationJob?.cancel()
        exifObservationJob = viewModelScope.launch {
            exifExtractWorkScheduler
                .observeUniqueWork(DefaultMediaStoreDiscoveryWorkScheduler.SOURCE_ID)
                .collect { infos ->
                    applyExifWorkInfos(infos)
                }
        }
    }

    private fun observeScreenshotOcrWork() {
        ocrObservationJob?.cancel()
        ocrObservationJob = viewModelScope.launch {
            screenshotOcrWorkScheduler
                .observeUniqueWork(DefaultMediaStoreDiscoveryWorkScheduler.SOURCE_ID)
                .collect { infos ->
                    applyScreenshotOcrWorkInfos(infos)
                }
        }
    }

    private suspend fun applyDiscoveryWorkInfos(infos: List<WorkInfo>) {
        if (infos.isEmpty()) return
        if (mutableUiState.value.photoAccess != PhotoAccessState.GRANTED &&
            mutableUiState.value.indexing != MediaStoreIndexingState.IN_PROGRESS
        ) {
            return
        }

        val sourceId = DefaultMediaStoreDiscoveryWorkScheduler.SOURCE_ID
        val photoCount = runCatching {
            assetRepository.countBySourceAndType(sourceId, AssetType.PHOTO)
        }.getOrDefault(0)
        val screenshotCount = runCatching {
            assetRepository.countBySourceAndType(sourceId, AssetType.SCREENSHOT)
        }.getOrDefault(0)
        val totalAssets = photoCount + screenshotCount

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
                mutableUiState.value = mutableUiState.value.copy(
                    photoAccess = PhotoAccessState.GRANTED,
                    indexing = MediaStoreIndexingState.COMPLETED(
                        discoveredAssetCount = totalAssets,
                        hasMore = hasMore,
                        accessScope = accessScope,
                    ),
                    screenshotCatalogueCount = screenshotCount,
                )
            }
        }
    }

    private suspend fun applyExifWorkInfos(infos: List<WorkInfo>) {
        if (infos.isEmpty()) return
        if (mutableUiState.value.photoAccess != PhotoAccessState.GRANTED) return

        when {
            infos.any { info ->
                info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED
            } -> {
                mutableUiState.value = mutableUiState.value.copy(
                    exifExtract = ImageExifExtractUiState.InProgress,
                )
            }

            infos.any { it.state == WorkInfo.State.FAILED } -> {
                val failed = infos.lastOrNull { it.state == WorkInfo.State.FAILED }
                val reason = failed?.outputData
                    ?.getString(MediaStoreImageExifExtractWorker.KEY_FAILURE_REASON)
                if (reason == MediaStoreImageExifExtractWorker.REASON_ACCESS_STOPPED) {
                    mutableUiState.value = mutableUiState.value.copy(
                        photoAccess = PhotoAccessState.REQUIRED,
                        exifExtract = ImageExifExtractUiState.AccessStopped,
                    )
                } else {
                    mutableUiState.value = mutableUiState.value.copy(
                        exifExtract = ImageExifExtractUiState.Failed(
                            "Memora could not finish reading photo facts. You can try again.",
                        ),
                    )
                }
            }

            infos.all { it.state.isFinished } -> {
                val sourceId = DefaultMediaStoreDiscoveryWorkScheduler.SOURCE_ID
                val catalogueCount = runCatching {
                    assetRepository.countBySourceAndType(sourceId, AssetType.PHOTO) +
                        assetRepository.countBySourceAndType(sourceId, AssetType.SCREENSHOT)
                }.getOrDefault(0)
                val screenshotCount = runCatching {
                    assetRepository.countBySourceAndType(sourceId, AssetType.SCREENSHOT)
                }.getOrDefault(0)
                val extractedCount = runCatching {
                    imageExifPersistence.countCurrentForSource(
                        sourceId = sourceId.value,
                        schemaVersion = RunPendingImageExifExtract.SCHEMA.value,
                    )
                }.getOrDefault(0)
                mutableUiState.value = mutableUiState.value.copy(
                    exifExtract = ImageExifExtractUiState.Completed(
                        extractedCount = extractedCount,
                        catalogueCount = catalogueCount,
                    ),
                    screenshotCatalogueCount = screenshotCount,
                    screenshotOcr = ScreenshotOcrExtractUiState.NotStarted,
                )
            }
        }
    }

    private suspend fun applyScreenshotOcrWorkInfos(infos: List<WorkInfo>) {
        if (infos.isEmpty()) return
        if (mutableUiState.value.photoAccess != PhotoAccessState.GRANTED) return

        when {
            infos.any { info ->
                info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED
            } -> {
                mutableUiState.value = mutableUiState.value.copy(
                    screenshotOcr = ScreenshotOcrExtractUiState.InProgress,
                )
            }

            infos.any { it.state == WorkInfo.State.FAILED } -> {
                val failed = infos.lastOrNull { it.state == WorkInfo.State.FAILED }
                val reason = failed?.outputData
                    ?.getString(MediaStoreScreenshotOcrExtractWorker.KEY_FAILURE_REASON)
                if (reason == MediaStoreScreenshotOcrExtractWorker.REASON_ACCESS_STOPPED) {
                    // Keep photoAccess as-is so the user can retry without wiping catalogue/EXIF.
                    // True revoke is re-detected on the next OCR attempt.
                    mutableUiState.value = mutableUiState.value.copy(
                        screenshotOcr = ScreenshotOcrExtractUiState.AccessStopped,
                    )
                } else {
                    mutableUiState.value = mutableUiState.value.copy(
                        screenshotOcr = ScreenshotOcrExtractUiState.Failed(
                            "Memora could not finish reading text from screenshots. You can try again.",
                        ),
                    )
                }
            }

            infos.all { it.state.isFinished } -> {
                val sourceId = DefaultMediaStoreDiscoveryWorkScheduler.SOURCE_ID
                val screenshotCount = runCatching {
                    assetRepository.countBySourceAndType(sourceId, AssetType.SCREENSHOT)
                }.getOrDefault(0)
                val extractedCount = runCatching {
                    screenshotOcrPersistence.countCurrentForSource(
                        sourceId = sourceId.value,
                        schemaVersion = RunPendingScreenshotOcrExtract.SCHEMA.value,
                    )
                }.getOrDefault(0)
                mutableUiState.value = mutableUiState.value.copy(
                    screenshotCatalogueCount = screenshotCount,
                    screenshotOcr = ScreenshotOcrExtractUiState.Completed(
                        extractedCount = extractedCount,
                        screenshotCatalogueCount = screenshotCount,
                    ),
                )
            }
        }
    }
}
