package com.memora.app.ui.setup

/**
 * Pure presentation session for ADR-017 visible recovery controls.
 *
 * It never opens a descriptor, binds the parser, or persists extraction text.
 * The ViewModel owns when a real status-only parse is launched.
 */
class PdfLocalReadingSession {
    var state: PdfLocalReadingState = PdfLocalReadingState.NeedsExplanation
        private set

    fun onEvent(event: PdfLocalReadingEvent): PdfLocalReadingState {
        state = when (val current = state) {
            PdfLocalReadingState.NeedsExplanation -> when (event) {
                PdfLocalReadingEvent.AcknowledgeScope -> PdfLocalReadingState.Ready
                else -> current
            }

            PdfLocalReadingState.Ready -> when (event) {
                PdfLocalReadingEvent.Start -> PdfLocalReadingState.InProgress
                PdfLocalReadingEvent.ShowRetryableDemo -> PdfLocalReadingState.RetryableProblem
                PdfLocalReadingEvent.ShowAccessRecoveryDemo -> PdfLocalReadingState.AccessRecoveryNeeded
                PdfLocalReadingEvent.ShowPasswordDemo -> PdfLocalReadingState.PasswordProtected
                else -> current
            }

            PdfLocalReadingState.InProgress -> when (event) {
                PdfLocalReadingEvent.Pause -> PdfLocalReadingState.Paused
                PdfLocalReadingEvent.Stop -> PdfLocalReadingState.Ready
                PdfLocalReadingEvent.FinishOk -> PdfLocalReadingState.Completed
                PdfLocalReadingEvent.FailRetryable -> PdfLocalReadingState.RetryableProblem
                PdfLocalReadingEvent.FailAccessRevoked -> PdfLocalReadingState.AccessRecoveryNeeded
                PdfLocalReadingEvent.FailPassword -> PdfLocalReadingState.PasswordProtected
                else -> current
            }

            PdfLocalReadingState.Paused -> when (event) {
                PdfLocalReadingEvent.Resume -> PdfLocalReadingState.InProgress
                PdfLocalReadingEvent.Stop -> PdfLocalReadingState.Ready
                else -> current
            }

            PdfLocalReadingState.RetryableProblem -> when (event) {
                PdfLocalReadingEvent.Retry -> PdfLocalReadingState.InProgress
                PdfLocalReadingEvent.Stop -> PdfLocalReadingState.Ready
                else -> current
            }

            PdfLocalReadingState.Completed,
            PdfLocalReadingState.AccessRecoveryNeeded,
            PdfLocalReadingState.PasswordProtected,
            PdfLocalReadingState.Unavailable,
            -> when (event) {
                PdfLocalReadingEvent.Stop -> PdfLocalReadingState.Ready
                PdfLocalReadingEvent.AcknowledgeScope -> PdfLocalReadingState.NeedsExplanation
                else -> current
            }
        }
        return state
    }
}

sealed interface PdfLocalReadingState {
    data object NeedsExplanation : PdfLocalReadingState

    data object Ready : PdfLocalReadingState

    data object InProgress : PdfLocalReadingState

    data object Paused : PdfLocalReadingState

    data object Completed : PdfLocalReadingState

    data object RetryableProblem : PdfLocalReadingState

    data object AccessRecoveryNeeded : PdfLocalReadingState

    data object PasswordProtected : PdfLocalReadingState

    data object Unavailable : PdfLocalReadingState
}

sealed interface PdfLocalReadingEvent {
    data object AcknowledgeScope : PdfLocalReadingEvent

    data object Start : PdfLocalReadingEvent

    data object Pause : PdfLocalReadingEvent

    data object Resume : PdfLocalReadingEvent

    data object Stop : PdfLocalReadingEvent

    data object Retry : PdfLocalReadingEvent

    data object FinishOk : PdfLocalReadingEvent

    data object FailRetryable : PdfLocalReadingEvent

    data object FailAccessRevoked : PdfLocalReadingEvent

    data object FailPassword : PdfLocalReadingEvent

    data object ShowRetryableDemo : PdfLocalReadingEvent

    data object ShowAccessRecoveryDemo : PdfLocalReadingEvent

    data object ShowPasswordDemo : PdfLocalReadingEvent
}
