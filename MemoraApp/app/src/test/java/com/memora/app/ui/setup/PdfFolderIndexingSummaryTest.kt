package com.memora.app.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfFolderIndexingSummaryTest {
    @Test
    fun idle_summary_for_returning_users_offers_check_for_new_pdfs() {
        assertEquals(
            "5 PDF items already listed in this connected folder. Tap Check for new PDFs after " +
                "you add files. Text search still needs Local PDF reading for each document.",
            idlePdfFolderIndexingSummary(totalAssetCount = 5),
        )
    }

    @Test
    fun `first completed drain states totals and local reading next step`() {
        assertEquals(
            "Found 3 new PDFs this pass. 3 PDF items in this connected folder. " +
                "This folder list is up to date for now. Text search still needs Local PDF reading " +
                "for each document. Tap Check for new PDFs after you add files.",
            completedPdfFolderIndexingSummary(
                totalAssetCount = 3,
                newlyDiscoveredAssetCount = 3,
                hasMore = false,
            ),
        )
    }

    @Test
    fun `rescan with no new files is explicit`() {
        assertEquals(
            "No new PDFs were listed this pass. 3 PDF items in this connected folder. " +
                "This folder list is up to date for now. Text search still needs Local PDF reading " +
                "for each document. Tap Check for new PDFs after you add files.",
            completedPdfFolderIndexingSummary(
                totalAssetCount = 3,
                newlyDiscoveredAssetCount = 0,
                hasMore = false,
            ),
        )
    }

    @Test
    fun `incomplete drain asks the user to continue metadata listing without claiming text search`() {
        assertEquals(
            "Found 2 new PDFs this pass. 2 PDF items in this connected folder. " +
                "More folder metadata remains. Tap Continue folder indexing to keep listing documents. " +
                "This does not save PDF text for search yet.",
            completedPdfFolderIndexingSummary(
                totalAssetCount = 2,
                newlyDiscoveredAssetCount = 2,
                hasMore = true,
            ),
        )
    }
}
