package com.memora.app.ui.setup

import com.memora.app.application.documents.PdfLocalReadingStatusCheckResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfLocalReadingStatusMappingTest {
    @Test
    fun maps_terminal_status_results_to_session_events() {
        assertEquals(
            PdfLocalReadingEvent.FinishOk,
            pdfLocalReadingEventFor(PdfLocalReadingStatusCheckResult.Completed),
        )
        assertEquals(
            PdfLocalReadingEvent.FailPassword,
            pdfLocalReadingEventFor(PdfLocalReadingStatusCheckResult.PasswordProtected),
        )
        assertEquals(
            PdfLocalReadingEvent.FailAccessRevoked,
            pdfLocalReadingEventFor(PdfLocalReadingStatusCheckResult.AccessRecoveryNeeded),
        )
        assertEquals(
            PdfLocalReadingEvent.FailRetryable,
            pdfLocalReadingEventFor(PdfLocalReadingStatusCheckResult.RetryableProblem),
        )
    }

    @Test
    fun cancelled_result_is_ignored_by_session() {
        assertNull(pdfLocalReadingEventFor(PdfLocalReadingStatusCheckResult.Cancelled))
    }
}
