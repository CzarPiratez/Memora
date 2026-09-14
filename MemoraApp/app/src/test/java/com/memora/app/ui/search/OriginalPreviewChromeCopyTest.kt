package com.memora.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginalPreviewChromeCopyTest {
    @Test
    fun icon_names_are_plain_language_for_talkback() {
        val copy = listOf(
            OriginalPreviewChromeCopy.INFO_LABEL,
            OriginalPreviewChromeCopy.INFO_DISMISS_LABEL,
        ).joinToString("\n").lowercase()
        assertTrue(copy.contains("about this preview"))
        assertFalse(copy.contains("dialog"))
        assertFalse(copy.contains("modal"))
        assertFalse(copy.contains("sheet"))
        assertFalse(copy.contains("metadata"))
    }

    /**
     * Back, Share, and info are icon-only. Open keeps a word because no glyph
     * reliably means "open this in another app" to someone who is not an
     * engineer.
     */
    @Test
    fun open_is_the_only_labelled_action_and_it_is_one_word() {
        assertEquals("Open", OriginalHandoffCopy.OPEN_LABEL)
        assertFalse(OriginalHandoffCopy.OPEN_LABEL.contains(" "))
    }

    @Test
    fun the_storage_hash_never_reaches_the_person() {
        assertEquals(
            "Spelling list 4 (1)-1.pdf",
            CanonicalRecallWhyCopy.friendlyDisplayLabel(
                "500fb02768e7a4bc0b91977e9d346a14774802600_Spelling list 4 (1)-1.pdf",
            ),
        )
    }
}
