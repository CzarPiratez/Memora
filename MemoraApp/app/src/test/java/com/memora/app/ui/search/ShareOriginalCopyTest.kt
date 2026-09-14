package com.memora.app.ui.search

import com.memora.app.application.preview.OriginalPreviewReloadRequest
import com.memora.app.application.share.ShareOriginalRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareOriginalCopyTest {
    @Test
    fun share_copy_is_on_device_and_not_act_or_upload() {
        val copy = listOf(
            ShareOriginalCopy.SHARE_LABEL,
            ShareOriginalCopy.HINT_BODY,
            ShareOriginalCopy.SOURCE_UNAVAILABLE_BODY,
            ShareOriginalCopy.COULD_NOT_SHARE_BODY,
        ).joinToString("\n").lowercase()
        assertTrue(copy.contains("share"))
        assertTrue(copy.contains("does not upload"))
        assertTrue(copy.contains("whole file"))
        assertFalse(copy.contains("act"))
        assertFalse(copy.contains("available"))
        assertFalse(copy.contains("embedding"))
        assertFalse(copy.contains("openai"))
    }

    @Test
    fun pdf_preview_share_drops_the_page_because_the_file_is_the_original() {
        val share = OriginalPreviewReloadRequest.Pdf(
            sourceId = "s",
            sourceAssetKey = "k",
            pageNumber = 9,
            documentLabel = "pool.pdf",
        ).toShareRequest()
        val pdf = share as ShareOriginalRequest.Pdf
        assertEquals("pool.pdf", pdf.label)
        assertEquals("s", pdf.sourceId)
    }
}
