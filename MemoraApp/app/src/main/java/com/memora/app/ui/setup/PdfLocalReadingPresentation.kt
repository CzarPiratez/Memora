package com.memora.app.ui.setup

/** Maps local-reading UI state to jargon-free body copy. */
fun pdfLocalReadingBody(state: PdfLocalReadingState): String = when (state) {
    PdfLocalReadingState.NeedsExplanation -> PdfLocalReadingCopy.SCOPE_BODY
    PdfLocalReadingState.Ready -> PdfLocalReadingCopy.READY_BODY
    PdfLocalReadingState.InProgress -> PdfLocalReadingCopy.IN_PROGRESS_BODY
    PdfLocalReadingState.Paused -> PdfLocalReadingCopy.PAUSED_BODY
    PdfLocalReadingState.Completed -> PdfLocalReadingCopy.COMPLETED_BODY
    PdfLocalReadingState.RetryableProblem -> PdfLocalReadingCopy.RETRYABLE_BODY
    PdfLocalReadingState.AccessRecoveryNeeded -> PdfLocalReadingCopy.ACCESS_RECOVERY_BODY
    PdfLocalReadingState.PasswordProtected -> PdfLocalReadingCopy.PASSWORD_BODY
    PdfLocalReadingState.Unavailable -> PdfLocalReadingCopy.UNAVAILABLE_BODY
}
