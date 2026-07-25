package com.memora.app.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PdfLocalReadingCopyTest {
    @Test
    fun all_user_copy_avoids_implementation_jargon() {
        val copy = listOf(
            PdfLocalReadingCopy.SECTION_TITLE,
            PdfLocalReadingCopy.SCOPE_BODY,
            PdfLocalReadingCopy.READY_BODY,
            PdfLocalReadingCopy.IN_PROGRESS_BODY,
            PdfLocalReadingCopy.PAUSED_BODY,
            PdfLocalReadingCopy.COMPLETED_BODY,
            PdfLocalReadingCopy.RETRYABLE_BODY,
            PdfLocalReadingCopy.ACCESS_RECOVERY_BODY,
            PdfLocalReadingCopy.PASSWORD_BODY,
            PdfLocalReadingCopy.UNAVAILABLE_BODY,
            PdfLocalReadingCopy.RECONNECT_HINT,
        ).joinToString("\n")

        val lower = copy.lowercase()
        assertFalse(lower.contains("sqlcipher"))
        assertFalse(lower.contains("encrypt"))
        assertFalse(lower.contains("keystore"))
        assertFalse(lower.contains("passphrase"))
        assertFalse(lower.contains("cipher"))
        assertFalse(lower.contains("binder"))
        assertFalse(lower.contains("aidl"))
        assertFalse(lower.contains("isolated process"))
        assertFalse(lower.contains("pdfbox"))
        assertFalse(Regex("""\bkeys?\b""").containsMatchIn(lower))
    }

    @Test
    fun ready_and_completed_copy_state_search_save_on_this_phone() {
        assertEquals(
            "Start reads indexed PDFs from your connected folder on this phone in the background " +
                "and, when reading succeeds, saves their text for search on this phone.",
            PdfLocalReadingCopy.READY_BODY,
        )
        assertEquals(
            "Memora finished reading PDFs from this folder and saved text for search on this phone " +
                "where reading succeeded. Your originals are unchanged.",
            PdfLocalReadingCopy.COMPLETED_BODY,
        )
    }
}
