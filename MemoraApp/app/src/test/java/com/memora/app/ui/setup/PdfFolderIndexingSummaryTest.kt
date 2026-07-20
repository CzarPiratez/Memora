package com.memora.app.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfFolderIndexingSummaryTest {
    @Test
    fun `completed folder page states the bounded count and completion`() {
        assertEquals(
            "Memora indexed 1 PDF item from this connected folder. This connected folder is currently up to date.",
            completedPdfFolderIndexingSummary(discoveredAssetCount = 1, hasMore = false),
        )
    }

    @Test
    fun `incomplete folder page makes the next explicit action clear`() {
        assertEquals(
            "Memora indexed 2 PDF items from this connected folder. More folder metadata remains. You can choose another indexing step later.",
            completedPdfFolderIndexingSummary(discoveredAssetCount = 2, hasMore = true),
        )
    }
}
