package com.memora.app.ui.setup

import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.documents.RunPdfLocalReadingStatusCheck
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Local PDF reading ViewModel: presentation session plus one foreground status-only parse.
 *
 * It never persists extraction text, schedules WorkManager, invokes AI, or uses the network.
 */
@HiltViewModel
class PdfLocalReadingViewModel @Inject constructor(
    private val runStatusCheck: RunPdfLocalReadingStatusCheck,
) : ViewModel() {
    private val session = PdfLocalReadingSession()
    private val mutableUiState = MutableStateFlow(session.state)
    private var activeJob: Job? = null
    private var activeCancellation: CancellationSignal? = null

    val uiState: StateFlow<PdfLocalReadingState> = mutableUiState.asStateFlow()

    fun onAcknowledgeScope() = dispatch(PdfLocalReadingEvent.AcknowledgeScope)

    fun onStart() {
        dispatch(PdfLocalReadingEvent.Start)
        if (mutableUiState.value == PdfLocalReadingState.InProgress) {
            beginStatusCheck()
        }
    }

    fun onPause() {
        cancelActiveWork()
        dispatch(PdfLocalReadingEvent.Pause)
    }

    fun onResume() {
        dispatch(PdfLocalReadingEvent.Resume)
        if (mutableUiState.value == PdfLocalReadingState.InProgress) {
            beginStatusCheck()
        }
    }

    fun onStop() {
        cancelActiveWork()
        dispatch(PdfLocalReadingEvent.Stop)
    }

    fun onRetry() {
        dispatch(PdfLocalReadingEvent.Retry)
        if (mutableUiState.value == PdfLocalReadingState.InProgress) {
            beginStatusCheck()
        }
    }

    fun onShowRetryableDemo() = dispatch(PdfLocalReadingEvent.ShowRetryableDemo)

    private fun beginStatusCheck() {
        cancelActiveWork()
        val cancellationSignal = CancellationSignal()
        activeCancellation = cancellationSignal
        activeJob = viewModelScope.launch {
            val result = runStatusCheck(cancellationSignal)
            if (cancellationSignal.isCanceled) {
                return@launch
            }
            if (mutableUiState.value != PdfLocalReadingState.InProgress) {
                return@launch
            }
            val event = pdfLocalReadingEventFor(result) ?: return@launch
            dispatch(event)
        }
    }

    private fun cancelActiveWork() {
        activeCancellation?.cancel()
        activeCancellation = null
        activeJob?.cancel()
        activeJob = null
    }

    private fun dispatch(event: PdfLocalReadingEvent) {
        mutableUiState.value = session.onEvent(event)
    }

    override fun onCleared() {
        cancelActiveWork()
        super.onCleared()
    }
}
