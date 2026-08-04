package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.memora.app.application.documents.PdfFolderConnectionFinder
import com.memora.app.application.documents.RunPendingPdfLocalReading
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.SourceId
import com.memora.app.work.SafPdfExtractWorkScheduler
import com.memora.app.work.SafPdfExtractWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Local PDF reading ViewModel: presentation session plus WorkManager-backed extract drain.
 *
 * Explicit Start enqueues unique extract work. Never invokes AI or uses the network.
 */
@HiltViewModel
class PdfLocalReadingViewModel @Inject constructor(
    private val findPdfFolderConnection: PdfFolderConnectionFinder,
    private val extractWorkScheduler: SafPdfExtractWorkScheduler,
    private val assetRepository: AssetRepository,
) : ViewModel() {
    private val session = PdfLocalReadingSession()
    private val mutableUiState = MutableStateFlow(PdfLocalReadingUiState())
    private var workObservationJob: Job? = null
    private var observedSourceId: SourceId? = null
    private var pendingAtStart: Int = 0

    val uiState: StateFlow<PdfLocalReadingUiState> = mutableUiState.asStateFlow()

    fun onAcknowledgeScope() = dispatch(PdfLocalReadingEvent.AcknowledgeScope)

    fun onStart() {
        dispatch(PdfLocalReadingEvent.Start)
        if (mutableUiState.value.phase == PdfLocalReadingState.InProgress) {
            beginExtractDrain()
        }
    }

    fun onPause() {
        observedSourceId?.let(extractWorkScheduler::cancel)
        dispatch(PdfLocalReadingEvent.Pause)
    }

    fun onResume() {
        dispatch(PdfLocalReadingEvent.Resume)
        if (mutableUiState.value.phase == PdfLocalReadingState.InProgress) {
            beginExtractDrain()
        }
    }

    fun onStop() {
        observedSourceId?.let(extractWorkScheduler::cancel)
        dispatch(PdfLocalReadingEvent.Stop)
    }

    /** Clears session and cancels extract observation after user-confirmed index clear. */
    fun onDerivedDataCleared() {
        workObservationJob?.cancel()
        workObservationJob = null
        observedSourceId?.let(extractWorkScheduler::cancel)
        observedSourceId = null
        pendingAtStart = 0
        mutableUiState.value = PdfLocalReadingUiState(
            phase = session.resetAfterDerivedDataCleared(),
        )
    }

    fun onRetry() {
        dispatch(PdfLocalReadingEvent.Retry)
        if (mutableUiState.value.phase == PdfLocalReadingState.InProgress) {
            beginExtractDrain()
        }
    }

    fun onShowRetryableDemo() = dispatch(PdfLocalReadingEvent.ShowRetryableDemo)

    private fun beginExtractDrain() {
        viewModelScope.launch {
            val sourceId = runCatching { findPdfFolderConnection() }.getOrNull()
            if (sourceId == null) {
                if (mutableUiState.value.phase == PdfLocalReadingState.InProgress) {
                    dispatch(PdfLocalReadingEvent.FailAccessRevoked)
                }
                return@launch
            }
            observedSourceId = sourceId
            pendingAtStart = runCatching {
                assetRepository.countPdfPendingLocalReading(
                    sourceId = sourceId,
                    schemaVersion = RunPendingPdfLocalReading.EXTRACTION_SCHEMA.value,
                )
            }.getOrDefault(0)
            publishProgress(drainedCount = 0)
            extractWorkScheduler.enqueueDrain(sourceId)
            observeExtractWork(sourceId)
        }
    }

    private fun observeExtractWork(sourceId: SourceId) {
        workObservationJob?.cancel()
        workObservationJob = viewModelScope.launch {
            extractWorkScheduler.observeUniqueWork(sourceId).collect { infos ->
                applyWorkInfos(infos)
            }
        }
    }

    private fun applyWorkInfos(infos: List<WorkInfo>) {
        if (infos.isEmpty()) return
        val phase = mutableUiState.value.phase
        if (phase != PdfLocalReadingState.InProgress &&
            phase != PdfLocalReadingState.Paused
        ) {
            return
        }
        if (phase == PdfLocalReadingState.Paused) return

        val drained = infos.count { info ->
            info.state == WorkInfo.State.SUCCEEDED &&
                info.outputData.getBoolean(SafPdfExtractWorker.KEY_UNIT_FINISHED, false)
        }
        publishProgress(drainedCount = drained)

        when {
            infos.any { info ->
                info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED
            } -> {
                if (mutableUiState.value.phase != PdfLocalReadingState.InProgress) {
                    dispatch(PdfLocalReadingEvent.Start)
                }
            }

            infos.any { it.state == WorkInfo.State.FAILED } -> {
                val failed = infos.lastOrNull { it.state == WorkInfo.State.FAILED }
                val reason = failed?.outputData?.getString(SafPdfExtractWorker.KEY_FAILURE_REASON)
                val event = if (reason == SafPdfExtractWorker.REASON_ACCESS_STOPPED) {
                    PdfLocalReadingEvent.FailAccessRevoked
                } else {
                    PdfLocalReadingEvent.FailRetryable
                }
                dispatch(event)
            }

            infos.all { it.state.isFinished } -> {
                dispatch(PdfLocalReadingEvent.FinishOk, drainedCount = drained)
            }
        }
    }

    private fun publishProgress(drainedCount: Int) {
        val current = mutableUiState.value
        if (current.phase != PdfLocalReadingState.InProgress &&
            current.phase != PdfLocalReadingState.Paused
        ) {
            return
        }
        mutableUiState.value = current.copy(
            drainedCount = drainedCount,
            pendingAtStart = pendingAtStart,
            progressFeedback = PdfLocalReadingCopy.progressFeedback(
                drainedCount = drainedCount,
                pendingAtStart = pendingAtStart,
            ),
        )
    }

    private fun dispatch(event: PdfLocalReadingEvent, drainedCount: Int? = null) {
        val phase = session.onEvent(event)
        val drained = drainedCount ?: mutableUiState.value.drainedCount
        mutableUiState.value = PdfLocalReadingUiState(
            phase = phase,
            drainedCount = drained,
            pendingAtStart = pendingAtStart,
            progressFeedback = when (phase) {
                PdfLocalReadingState.InProgress,
                PdfLocalReadingState.Paused,
                -> PdfLocalReadingCopy.progressFeedback(drained, pendingAtStart)
                PdfLocalReadingState.Completed -> null
                else -> null
            },
        )
    }

    override fun onCleared() {
        workObservationJob?.cancel()
        super.onCleared()
    }
}
