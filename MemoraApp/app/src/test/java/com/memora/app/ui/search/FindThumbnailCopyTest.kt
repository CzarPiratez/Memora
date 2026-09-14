package com.memora.app.ui.search

import com.memora.app.application.find.FindThumbnailGlyph
import com.memora.app.application.find.FindThumbnailRequest
import com.memora.app.application.find.FindThumbnailResult
import com.memora.app.domain.asset.AssetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FindThumbnailCopyTest {
    @Test
    fun cited_pdf_page_is_spoken_first_page_is_honest() {
        val request = FindThumbnailRequest(
            sourceId = "s",
            sourceAssetKey = "a",
            assetType = AssetType.PDF,
            label = "pool.pdf",
            pageNumber = 3,
        )
        val cited = FindThumbnailResult.Ready(1, 1, intArrayOf(0), 3, true)
        assertEquals("Page 3 of pool.pdf", FindThumbnailCopy.spokenDescription(request, cited))

        val first = FindThumbnailResult.Ready(1, 1, intArrayOf(0), 1, false)
        assertEquals("First page of pool.pdf", FindThumbnailCopy.spokenDescription(request, first))
    }

    @Test
    fun photo_pixels_are_decorative_beside_the_filename() {
        val request = FindThumbnailRequest(
            sourceId = "s",
            sourceAssetKey = "a",
            assetType = AssetType.PHOTO,
            label = "IMG_1.jpg",
            pageNumber = null,
        )
        val ready = FindThumbnailResult.Ready(2, 2, IntArray(4), null, false)
        assertNull(FindThumbnailCopy.spokenDescription(request, ready))
    }

    @Test
    fun note_glyph_does_not_pretend_to_be_a_page_image() {
        assertEquals("Note", FindThumbnailCopy.glyphLabel(FindThumbnailGlyph.NOTE))
        assertTrue(FindThumbnailCopy.UNAVAILABLE_DESCRIPTION.contains("unavailable"))
    }
}
