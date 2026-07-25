package com.memora.app.ui.setup

/**
 * Maps recovery session state to the plain-language body shown in the UI.
 */
fun pdfLocalReadingBody(state: PdfLocalReadingState): String = when (state) {
    PdfLocalReadingState.NeedsExplanation -> PdfLocalReadingCopy.SCOPE_BODY
    PdfLocalReadingState.Ready -> PdfLocalReadingCopy.READY_BODY
    PdfLocalReadingState.InProgress -> PdfLocalReadingCopy.IN_PROGRESS_BODY
    PdfLocalReadingState.Paused -> PdfLocalReadingCopy.PAUSED_BODY
    PdfLocalReadingState.RetryableProblem -> PdfLocalReadingCopy.RETRYABLE_BODY
    PdfLocalReadingState.AccessRecoveryNeeded -> PdfLocalReadingCopy.ACCESS_RECOVERY_BODY
    PdfLocalReadingState.PasswordProtected -> PdfLocalReadingCopy.PASSWORD_BODY
    PdfLocalReadingState.Unavailable -> PdfLocalReadingCopy.UNAVAILABLE_BODY
}
