package com.memora.app.application.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ClearMemoraDerivedDataCopyTest {
    @Test
    fun approved_rebuild_message_matches_adr_021_and_avoids_encryption_jargon() {
        val message = ClearMemoraDerivedData.APPROVED_REBUILD_MESSAGE
        assertEquals(
            "Your private Memora index needs to be rebuilt. Your original photos, documents, and notes are unchanged.",
            message,
        )
        val lower = message.lowercase()
        assertFalse(lower.contains("sqlcipher"))
        assertFalse(lower.contains("encrypt"))
        assertFalse(lower.contains("keystore"))
        assertFalse(lower.contains("passphrase"))
        assertFalse(Regex("""\bkeys?\b""").containsMatchIn(lower))
    }
}
