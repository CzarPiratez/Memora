package com.memora.app.ui.setup

import com.memora.app.domain.discovery.ImageLibraryAccessScope
import org.junit.Assert.assertEquals
import org.junit.Test

class IndexingSummaryTest {
    @Test
    fun `full-library completion states the bounded count and metadata-only honesty`() {
        assertEquals(
            "UNFYND indexed 1 item from your permitted photo library. " +
                "This permitted photo catalogue is currently up to date. " +
                "This lists metadata only; it does not read photo contents or create searchable memories yet.",
            completedIndexingSummary(
                discoveredAssetCount = 1,
                hasMore = false,
                accessScope = ImageLibraryAccessScope.FULL_LIBRARY,
            ),
        )
    }

    @Test
    fun `selected-photo completion never describes access as the full library`() {
        assertEquals(
            "UNFYND indexed 2 items from only the photos you selected. " +
                "More permitted items remain. Tap Start indexing to keep listing photo metadata. " +
                "This does not read photo contents or create searchable memories yet.",
            completedIndexingSummary(
                discoveredAssetCount = 2,
                hasMore = true,
                accessScope = ImageLibraryAccessScope.SELECTED_PHOTOS,
            ),
        )
    }

    @Test
    fun `zero-item page remains a truthful completed result`() {
        assertEquals(
            "UNFYND indexed 0 items from your permitted photo library. " +
                "This permitted photo catalogue is currently up to date. " +
                "This lists metadata only; it does not read photo contents or create searchable memories yet.",
            completedIndexingSummary(
                discoveredAssetCount = 0,
                hasMore = false,
                accessScope = ImageLibraryAccessScope.FULL_LIBRARY,
            ),
        )
    }

    @Test
    fun exif_completion_never_claims_ocr_or_search() {
        val summary = completedImageExifExtractSummary(
            extractedCount = 3,
            catalogueCount = 3,
        )
        assertEquals(
            "UNFYND saved basic facts for 3 photos (from 3 catalogued items). " +
                "This is EXIF and size metadata only — not OCR, keyword search, or meaning-based recall.",
            summary,
        )
        assertEquals(false, summary.lowercase().contains("searchable memories"))
    }

    @Test
    fun screenshot_ocr_completion_points_to_keyword_search_without_memory_claim() {
        val summary = completedScreenshotOcrExtractSummary(
            extractedCount = 1,
            screenshotCatalogueCount = 1,
        )
        assertEquals(
            "UNFYND saved on-device text from 1 screenshot " +
                "(from 1 catalogued screenshots). " +
                "This is OCR text only — not meaning-based recall. " +
                "You can search those words from Find saved screenshot text on the welcome screen. " +
                "Ordinary photos are not OCR’d in this step.",
            summary,
        )
        assertEquals(false, summary.lowercase().contains("meaning-based recall yet"))
        assertEquals(true, summary.contains("Find saved screenshot text"))
    }
}
