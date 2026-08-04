package com.memora.app.ui.setup

/** Maps local-reading UI state to jargon-free body copy. */
fun pdfLocalReadingBody(state: PdfLocalReadingUiState): String = when (state.phase) {
    PdfLocalReadingState.NeedsExplanation -> PdfLocalReadingCopy.SCOPE_BODY
    PdfLocalReadingState.Ready -> PdfLocalReadingCopy.READY_BODY
    PdfLocalReadingState.InProgress -> PdfLocalReadingCopy.IN_PROGRESS_BODY
    PdfLocalReadingState.Paused -> PdfLocalReadingCopy.PAUSED_BODY
    PdfLocalReadingState.Completed -> PdfLocalReadingCopy.completedBody(state.drainedCount)
    PdfLocalReadingState.RetryableProblem -> PdfLocalReadingCopy.RETRYABLE_BODY
    PdfLocalReadingState.AccessRecoveryNeeded -> PdfLocalReadingCopy.ACCESS_RECOVERY_BODY
    PdfLocalReadingState.PasswordProtected -> PdfLocalReadingCopy.PASSWORD_BODY
    PdfLocalReadingState.Unavailable -> PdfLocalReadingCopy.UNAVAILABLE_BODY
}

/** @deprecated Prefer [pdfLocalReadingBody] with [PdfLocalReadingUiState]. */
fun pdfLocalReadingBody(state: PdfLocalReadingState): String =
    pdfLocalReadingBody(PdfLocalReadingUiState(phase = state))
