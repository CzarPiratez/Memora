package com.memora.app.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfLocalReadingSessionTest {
    @Test
    fun acknowledge_start_pause_resume_stop_cycle() {
        val session = PdfLocalReadingSession()

        assertEquals(PdfLocalReadingState.NeedsExplanation, session.state)
        assertEquals(PdfLocalReadingState.Ready, session.onEvent(PdfLocalReadingEvent.AcknowledgeScope))
        assertEquals(PdfLocalReadingState.InProgress, session.onEvent(PdfLocalReadingEvent.Start))
        assertEquals(PdfLocalReadingState.Paused, session.onEvent(PdfLocalReadingEvent.Pause))
        assertEquals(PdfLocalReadingState.InProgress, session.onEvent(PdfLocalReadingEvent.Resume))
        assertEquals(PdfLocalReadingState.Ready, session.onEvent(PdfLocalReadingEvent.Stop))
    }

    @Test
    fun in_progress_success_reaches_completed_then_stop_returns_ready() {
        val session = PdfLocalReadingSession()
        session.onEvent(PdfLocalReadingEvent.AcknowledgeScope)
        session.onEvent(PdfLocalReadingEvent.Start)

        assertEquals(
            PdfLocalReadingState.Completed,
            session.onEvent(PdfLocalReadingEvent.FinishOk),
        )
        assertEquals(PdfLocalReadingState.Ready, session.onEvent(PdfLocalReadingEvent.Stop))
    }

    @Test
    fun retryable_failure_supports_retry_and_stop() {
        val session = PdfLocalReadingSession()
        session.onEvent(PdfLocalReadingEvent.AcknowledgeScope)
        session.onEvent(PdfLocalReadingEvent.Start)

        assertEquals(
            PdfLocalReadingState.RetryableProblem,
            session.onEvent(PdfLocalReadingEvent.FailRetryable),
        )
        assertEquals(PdfLocalReadingState.InProgress, session.onEvent(PdfLocalReadingEvent.Retry))
        session.onEvent(PdfLocalReadingEvent.FailRetryable)
        assertEquals(PdfLocalReadingState.Ready, session.onEvent(PdfLocalReadingEvent.Stop))
    }

    @Test
    fun start_before_acknowledge_is_ignored() {
        val session = PdfLocalReadingSession()

        assertEquals(
            PdfLocalReadingState.NeedsExplanation,
            session.onEvent(PdfLocalReadingEvent.Start),
        )
    }

    @Test
    fun demo_retryable_path_still_available_from_ready() {
        val session = PdfLocalReadingSession()
        session.onEvent(PdfLocalReadingEvent.AcknowledgeScope)

        assertEquals(
            PdfLocalReadingState.RetryableProblem,
            session.onEvent(PdfLocalReadingEvent.ShowRetryableDemo),
        )
    }

    @Test
    fun reset_after_derived_data_cleared_returns_to_needs_explanation() {
        val session = PdfLocalReadingSession()
        session.onEvent(PdfLocalReadingEvent.AcknowledgeScope)
        session.onEvent(PdfLocalReadingEvent.Start)
        session.onEvent(PdfLocalReadingEvent.FinishOk)
        assertEquals(PdfLocalReadingState.Completed, session.state)

        assertEquals(
            PdfLocalReadingState.NeedsExplanation,
            session.resetAfterDerivedDataCleared(),
        )
        assertEquals(PdfLocalReadingState.NeedsExplanation, session.state)
    }
}
