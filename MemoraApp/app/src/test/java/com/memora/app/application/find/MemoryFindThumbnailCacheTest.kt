package com.memora.app.application.find

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class MemoryFindThumbnailCacheTest {
    @Test
    fun stores_ready_pixels_and_evicts_oldest_after_cap() {
        val cache = MemoryFindThumbnailCache()
        val first = FindThumbnailResult.Ready(1, 1, intArrayOf(1), null, false)
        cache.put("a", first)
        assertSame(first, cache.get("a"))

        repeat(FindThumbnailLimits.CACHE_ENTRIES) { index ->
            cache.put(
                "k$index",
                FindThumbnailResult.Ready(1, 1, intArrayOf(index), null, false),
            )
        }
        assertNull(cache.get("a"))
        assertEquals(FindThumbnailLimits.CACHE_ENTRIES, cache.sizeForTest())
    }

    @Test
    fun unavailable_glyphs_are_not_stored() {
        val cache = MemoryFindThumbnailCache()
        cache.put("miss", FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE))
        assertNull(cache.get("miss"))
        assertEquals(0, cache.sizeForTest())
    }

    @Test
    fun clear_drops_every_entry() {
        val cache = MemoryFindThumbnailCache()
        cache.put("note", FindThumbnailResult.Glyph(FindThumbnailGlyph.NOTE))
        cache.clear()
        assertNull(cache.get("note"))
    }
}
