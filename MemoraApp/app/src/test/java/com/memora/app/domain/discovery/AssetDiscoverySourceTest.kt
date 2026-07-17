package com.memora.app.domain.discovery

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetDiscoverySourceTest {
    @Test
    fun `discovery page preserves a source-owned incremental checkpoint`() {
        val sourceId = SourceId("android-media-store")
        val page = DiscoveryPage(
            sourceId = sourceId,
            assets = listOf(asset(sourceId, "image-1")),
            checkpoint = DiscoveryCursor(sourceId, "modified:1720000000000"),
            hasMore = true,
        )

        assertFalse(page.isComplete)
    }

    @Test
    fun `completed discovery page keeps a checkpoint for the next incremental pass`() {
        val sourceId = SourceId("android-media-store")
        val page = DiscoveryPage(
            sourceId = sourceId,
            assets = emptyList(),
            checkpoint = DiscoveryCursor(sourceId, "modified:1720000000000"),
            hasMore = false,
        )

        assertTrue(page.isComplete)
    }

    @Test
    fun `discovery rejects a mismatched source duplicate identity and invalid batch`() {
        val mediaStore = SourceId("android-media-store")

        assertThrows(IllegalArgumentException::class.java) {
            DiscoveryPage(
                sourceId = mediaStore,
                assets = listOf(asset(SourceId("document-tree"), "document-1")),
                checkpoint = DiscoveryCursor(mediaStore, "cursor-1"),
                hasMore = false,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiscoveryPage(
                sourceId = mediaStore,
                assets = listOf(asset(mediaStore, "image-1"), asset(mediaStore, "image-1")),
                checkpoint = DiscoveryCursor(mediaStore, "cursor-1"),
                hasMore = false,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiscoveryRequest(batchSize = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiscoveryRequest(batchSize = 101)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiscoveryPage(
                sourceId = mediaStore,
                assets = emptyList(),
                checkpoint = DiscoveryCursor(SourceId("document-tree"), "cursor-1"),
                hasMore = false,
            )
        }
    }

    private fun asset(sourceId: SourceId, key: String): Asset = Asset(
        identity = AssetIdentity(sourceId, SourceAssetKey(key)),
        type = AssetType.PHOTO,
        location = AssetLocation("content://example/$key"),
        fingerprint = AssetFingerprint("fingerprint:$key"),
        discoveredAt = Instant.parse("2026-07-18T10:00:00Z"),
    )
}
