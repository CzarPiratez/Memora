package com.memora.app.domain.intelligence

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnfyndSelfCaptureTest {
    @Test
    fun two_chrome_phrases_are_a_self_capture() {
        assertTrue(
            UnfyndSelfCapture.matches(
                label = "Screenshot_20260904_124145.png",
                text = "What are you trying to remember? Search by meaning on this phone PDF memory",
            ),
        )
    }

    @Test
    fun unfynd_filename_plus_one_chrome_phrase_is_a_self_capture() {
        assertTrue(
            UnfyndSelfCapture.matches(
                label = "Screenshot_20260904_124145_UNFYND.png",
                text = "What are you trying to remember? files have swimming timetable",
            ),
        )
    }

    @Test
    fun a_real_timetable_is_not_a_self_capture() {
        assertFalse(
            UnfyndSelfCapture.matches(
                label = "Grade-2-Swimming-TT-2026.pdf",
                text = "Grade 2 Swimming Timetable 2026 PERIOD TIME MON TUE",
            ),
        )
    }

    @Test
    fun one_stray_chrome_phrase_in_a_real_file_is_not_enough() {
        assertFalse(
            UnfyndSelfCapture.matches(
                label = "meeting-notes.pdf",
                text = "Please open original invoices in the shared folder",
            ),
        )
    }
}
