package com.memora.app.application.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ClearMemoraDerivedDataCopyTest {
    @Test
    fun approved_rebuild_message_matches_adr_021_and_avoids_encryption_jargon() {
        val message = ClearMemoraDerivedData.APPROVED_REBUILD_MESSAGE
        assertEquals(
            "Your private UNFYND index needs to be rebuilt. Your original photos, documents, and notes are unchanged.",
            message,
        )
        assertNoForbiddenTerms(message)
    }

    @Test
    fun unlock_required_copy_avoids_encryption_jargon() {
        val copy = listOf(
            "Unlock your phone",
            "UNFYND opens your private index only after you unlock this phone. " +
                "Your original photos, documents, and notes stay where they are.",
            "Unlock, then UNFYND continues automatically.",
        ).joinToString("\n")
        assertNoForbiddenTerms(copy)
    }

    @Test
    fun opening_index_copy_does_not_ask_to_unlock() {
        val opening = "Opening UNFYND"
        assertFalse(opening.lowercase().contains("unlock"))
        assertNoForbiddenTerms(opening)
    }

    private fun assertNoForbiddenTerms(text: String) {
        val lower = text.lowercase()
        assertFalse(lower.contains("sqlcipher"))
        assertFalse(lower.contains("encrypt"))
        assertFalse(lower.contains("keystore"))
        assertFalse(lower.contains("passphrase"))
        assertFalse(lower.contains("cipher"))
        assertFalse(Regex("""\bkeys?\b""").containsMatchIn(lower))
    }
}
