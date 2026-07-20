package com.memora.app.ui.setup

import com.memora.app.domain.discovery.ImageLibraryAccessScope
import org.junit.Assert.assertEquals
import org.junit.Test

class IndexingSummaryTest {
    @Test
    fun `full-library completion states the bounded count and current status`() {
        assertEquals(
            "Memora indexed 1 item from your permitted photo library. This permitted source is currently up to date.",
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
            "Memora indexed 2 items from only the photos you selected. More permitted items remain. You can choose another indexing step later.",
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
            "Memora indexed 0 items from your permitted photo library. This permitted source is currently up to date.",
            completedIndexingSummary(
                discoveredAssetCount = 0,
                hasMore = false,
                accessScope = ImageLibraryAccessScope.FULL_LIBRARY,
            ),
        )
    }
}
