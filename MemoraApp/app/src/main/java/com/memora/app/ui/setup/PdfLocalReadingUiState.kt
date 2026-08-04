package com.memora.app.ui.setup

/**
 * UI model for local PDF reading: session phase plus aggregate drain progress.
 */
data class PdfLocalReadingUiState(
    val phase: PdfLocalReadingState = PdfLocalReadingState.NeedsExplanation,
    val progressFeedback: String? = null,
    val drainedCount: Int = 0,
    val pendingAtStart: Int = 0,
)
