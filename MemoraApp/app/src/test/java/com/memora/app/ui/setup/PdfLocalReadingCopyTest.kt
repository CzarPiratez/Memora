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
    fun scope_copy_states_local_read_only_folder_scope() {
        assertEquals(
            "When Memora reads PDF text, it will use only the folder you connected, " +
                "keep that access read-only, and leave your original files unchanged. " +
                "Reading stays on this phone.",
            PdfLocalReadingCopy.SCOPE_BODY,
        )
    }
}
