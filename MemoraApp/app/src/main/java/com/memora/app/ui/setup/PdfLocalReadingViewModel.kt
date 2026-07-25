package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.memora.app.application.documents.PdfFolderConnectionFinder
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
) : ViewModel() {
    private val session = PdfLocalReadingSession()
    private val mutableUiState = MutableStateFlow(session.state)
    private var workObservationJob: Job? = null
    private var observedSourceId: SourceId? = null

    val uiState: StateFlow<PdfLocalReadingState> = mutableUiState.asStateFlow()

    fun onAcknowledgeScope() = dispatch(PdfLocalReadingEvent.AcknowledgeScope)

    fun onStart() {
        dispatch(PdfLocalReadingEvent.Start)
        if (mutableUiState.value == PdfLocalReadingState.InProgress) {
            beginExtractDrain()
        }
    }

    fun onPause() {
        observedSourceId?.let(extractWorkScheduler::cancel)
        dispatch(PdfLocalReadingEvent.Pause)
    }

    fun onResume() {
        dispatch(PdfLocalReadingEvent.Resume)
        if (mutableUiState.value == PdfLocalReadingState.InProgress) {
            beginExtractDrain()
        }
    }

    fun onStop() {
        observedSourceId?.let(extractWorkScheduler::cancel)
        dispatch(PdfLocalReadingEvent.Stop)
    }

    fun onRetry() {
        dispatch(PdfLocalReadingEvent.Retry)
        if (mutableUiState.value == PdfLocalReadingState.InProgress) {
            beginExtractDrain()
        }
    }

    fun onShowRetryableDemo() = dispatch(PdfLocalReadingEvent.ShowRetryableDemo)

    private fun beginExtractDrain() {
        viewModelScope.launch {
            val sourceId = runCatching { findPdfFolderConnection() }.getOrNull()
            if (sourceId == null) {
                if (mutableUiState.value == PdfLocalReadingState.InProgress) {
                    dispatch(PdfLocalReadingEvent.FailAccessRevoked)
                }
                return@launch
            }
            observedSourceId = sourceId
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
        if (mutableUiState.value != PdfLocalReadingState.InProgress &&
            mutableUiState.value != PdfLocalReadingState.Paused
        ) {
            return
        }
        if (mutableUiState.value == PdfLocalReadingState.Paused) return

        when {
            infos.any { info ->
                info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED
            } -> {
                if (mutableUiState.value != PdfLocalReadingState.InProgress) {
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
                dispatch(PdfLocalReadingEvent.FinishOk)
            }
        }
    }

    private fun dispatch(event: PdfLocalReadingEvent) {
        mutableUiState.value = session.onEvent(event)
    }

    override fun onCleared() {
        workObservationJob?.cancel()
        super.onCleared()
    }
}
