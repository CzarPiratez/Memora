package com.memora.app.ui.search

import com.memora.app.application.handoff.OriginalHandoffRequest
import com.memora.app.application.preview.OriginalPreviewReloadRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginalHandoffCopyTest {
    @Test
    fun handoff_copy_is_on_device_and_not_act_or_upload() {
        val copy = listOf(
            OriginalHandoffCopy.SHARE_LABEL,
            OriginalHandoffCopy.OPEN_LABEL,
            OriginalHandoffCopy.HINT_BODY,
            OriginalHandoffCopy.SOURCE_UNAVAILABLE_BODY,
            OriginalHandoffCopy.COULD_NOT_SHARE_BODY,
            OriginalHandoffCopy.COULD_NOT_OPEN_BODY,
            OriginalHandoffCopy.NO_APP_BODY,
        ).joinToString("\n").lowercase()
        assertTrue(copy.contains("share"))
        assertTrue(copy.contains("does not upload"))
        assertTrue(copy.contains("whole file"))
        assertFalse(copy.contains("available."))
        assertFalse(copy.contains("embedding"))
        assertFalse(copy.contains("openai"))
    }

    @Test
    fun the_hint_promises_read_only_and_a_way_back() {
        val hint = OriginalHandoffCopy.HINT_BODY.lowercase()
        assertTrue(hint.contains("read-only"))
        assertTrue(hint.contains("does not upload or change"))
        assertTrue(hint.contains("back"))
    }

    @Test
    fun a_missing_reader_points_at_share_rather_than_blaming_the_file() {
        val body = OriginalHandoffCopy.NO_APP_BODY.lowercase()
        assertTrue(body.contains("no app on this phone"))
        assertTrue(body.contains("share"))
        assertFalse(body.contains("corrupt"))
    }

    @Test
    fun pdf_preview_handoff_drops_the_page_because_the_file_is_the_original() {
        val handoff = OriginalPreviewReloadRequest.Pdf(
            sourceId = "s",
            sourceAssetKey = "k",
            pageNumber = 9,
            documentLabel = "pool.pdf",
        ).toHandoffRequest()
        val pdf = handoff as OriginalHandoffRequest.Pdf
        assertEquals("pool.pdf", pdf.label)
        assertEquals("s", pdf.sourceId)
    }
}
