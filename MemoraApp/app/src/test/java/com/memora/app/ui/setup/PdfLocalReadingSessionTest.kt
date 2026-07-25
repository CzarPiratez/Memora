package com.memora.app.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfLocalReadingSessionTest {
    @Test
    fun acknowledge_then_start_pause_resume_and_stop() {
        val session = PdfLocalReadingSession()

        assertEquals(PdfLocalReadingState.NeedsExplanation, session.state)
        assertEquals(PdfLocalReadingState.Ready, session.onEvent(PdfLocalReadingEvent.AcknowledgeScope))
        assertEquals(PdfLocalReadingState.InProgress, session.onEvent(PdfLocalReadingEvent.Start))
        assertEquals(PdfLocalReadingState.Paused, session.onEvent(PdfLocalReadingEvent.Pause))
        assertEquals(PdfLocalReadingState.InProgress, session.onEvent(PdfLocalReadingEvent.Resume))
        assertEquals(PdfLocalReadingState.Ready, session.onEvent(PdfLocalReadingEvent.Stop))
    }

    @Test
    fun retryable_failure_can_retry_or_stop() {
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
    fun start_is_ignored_before_scope_is_acknowledged() {
        val session = PdfLocalReadingSession()

        assertEquals(
            PdfLocalReadingState.NeedsExplanation,
            session.onEvent(PdfLocalReadingEvent.Start),
        )
    }

    @Test
    fun demo_retryable_path_does_not_require_in_progress() {
        val session = PdfLocalReadingSession()
        session.onEvent(PdfLocalReadingEvent.AcknowledgeScope)

        assertEquals(
            PdfLocalReadingState.RetryableProblem,
            session.onEvent(PdfLocalReadingEvent.ShowRetryableDemo),
        )
    }

    @Test
    fun unavailable_is_an_honest_terminal_until_stop() {
        val session = PdfLocalReadingSession()
        session.onEvent(PdfLocalReadingEvent.AcknowledgeScope)
        session.onEvent(PdfLocalReadingEvent.Start)

        assertEquals(
            PdfLocalReadingState.Unavailable,
            session.onEvent(PdfLocalReadingEvent.MarkUnavailable),
        )
        assertEquals(PdfLocalReadingState.Ready, session.onEvent(PdfLocalReadingEvent.Stop))
    }
}
