package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Presentation-only ViewModel for ADR-017 visible PDF local-reading recovery.
 *
 * It never opens a PDF, binds the isolated parser for a user document, or writes
 * extraction text.
 */
@HiltViewModel
class PdfLocalReadingViewModel @Inject constructor() : ViewModel() {
    private val session = PdfLocalReadingSession()
    private val mutableUiState = MutableStateFlow(session.state)

    val uiState: StateFlow<PdfLocalReadingState> = mutableUiState.asStateFlow()

    fun onAcknowledgeScope() = dispatch(PdfLocalReadingEvent.AcknowledgeScope)

    fun onStart() = dispatch(PdfLocalReadingEvent.Start)

    fun onPause() = dispatch(PdfLocalReadingEvent.Pause)

    fun onResume() = dispatch(PdfLocalReadingEvent.Resume)

    fun onStop() = dispatch(PdfLocalReadingEvent.Stop)

    fun onRetry() = dispatch(PdfLocalReadingEvent.Retry)

    fun onShowRetryableDemo() = dispatch(PdfLocalReadingEvent.ShowRetryableDemo)

    fun onMarkUnavailable() = dispatch(PdfLocalReadingEvent.MarkUnavailable)

    private fun dispatch(event: PdfLocalReadingEvent) {
        mutableUiState.value = session.onEvent(event)
    }
}
