package com.memora.app.ui.setup

import com.memora.app.application.documents.PdfLocalReadingStatusCheckResult

/** Maps a content-free status-check result to a session event, or null when ignored. */
fun pdfLocalReadingEventFor(
    result: PdfLocalReadingStatusCheckResult,
): PdfLocalReadingEvent? = when (result) {
    PdfLocalReadingStatusCheckResult.Completed -> PdfLocalReadingEvent.FinishOk
    PdfLocalReadingStatusCheckResult.PasswordProtected -> PdfLocalReadingEvent.FailPassword
    PdfLocalReadingStatusCheckResult.AccessRecoveryNeeded -> PdfLocalReadingEvent.FailAccessRevoked
    PdfLocalReadingStatusCheckResult.RetryableProblem -> PdfLocalReadingEvent.FailRetryable
    PdfLocalReadingStatusCheckResult.Cancelled -> null
}
