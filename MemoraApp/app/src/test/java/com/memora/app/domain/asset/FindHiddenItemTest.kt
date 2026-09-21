package com.memora.app.domain.asset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class FindHiddenItemTest {
    @Test
    fun identity_is_source_plus_asset_key() {
        val item = FindHiddenItem(
            sourceId = SourceId("photos"),
            sourceAssetKey = SourceAssetKey("42"),
            label = "lake.jpg",
            hiddenAt = Instant.parse("2026-09-21T12:00:00Z"),
        )
        assertEquals(
            AssetIdentity(SourceId("photos"), SourceAssetKey("42")),
            item.asIdentity(),
        )
    }

    @Test
    fun blank_label_is_rejected() {
        assertThrows(IllegalArgumentException::class.java) {
            FindHiddenItem(
                sourceId = SourceId("photos"),
                sourceAssetKey = SourceAssetKey("42"),
                label = "  ",
                hiddenAt = Instant.parse("2026-09-21T12:00:00Z"),
            )
        }
    }
}
