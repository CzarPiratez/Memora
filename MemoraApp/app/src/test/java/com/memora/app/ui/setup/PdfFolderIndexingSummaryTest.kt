package com.memora.app.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfFolderIndexingSummaryTest {
    @Test
    fun `completed folder drain states the count and that text search still needs Local PDF reading`() {
        assertEquals(
            "Memora indexed 1 PDF item from this connected folder. " +
                "This connected folder's PDF list is currently up to date. " +
                "Text search still needs Local PDF reading for each document.",
            completedPdfFolderIndexingSummary(discoveredAssetCount = 1, hasMore = false),
        )
    }

    @Test
    fun `incomplete drain asks the user to continue metadata listing without claiming text search`() {
        assertEquals(
            "Memora indexed 2 PDF items from this connected folder. " +
                "More folder metadata remains. Tap Continue folder indexing to keep listing documents. " +
                "This does not save PDF text for search yet.",
            completedPdfFolderIndexingSummary(discoveredAssetCount = 2, hasMore = true),
        )
    }
}
