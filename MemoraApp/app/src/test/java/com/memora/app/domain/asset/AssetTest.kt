package com.memora.app.domain.asset

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AssetTest {
    @Test
    fun `identity distinguishes two assets from the same source`() {
        val sourceId = SourceId("android-media-store")

        val first = AssetIdentity(sourceId, SourceAssetKey("image-1"))
        val second = AssetIdentity(sourceId, SourceAssetKey("image-2"))

        assertNotEquals(first, second)
    }

    @Test
    fun `asset preserves immutable source identity and fingerprint`() {
        val asset = Asset(
            identity = AssetIdentity(
                sourceId = SourceId("android-media-store"),
                sourceAssetKey = SourceAssetKey("image-1"),
            ),
            type = AssetType.PHOTO,
            location = AssetLocation("content://media/external/images/media/1"),
            fingerprint = AssetFingerprint("media:1:1720000000000:2048"),
            discoveredAt = Instant.parse("2026-07-17T10:15:30Z"),
            displayName = "Lake.jpg",
        )

        assertEquals("android-media-store", asset.identity.sourceId.value)
        assertEquals("image-1", asset.identity.sourceAssetKey.value)
        assertEquals("media:1:1720000000000:2048", asset.fingerprint.value)
    }

    @Test
    fun `identity values reject blank input`() {
        assertThrows(IllegalArgumentException::class.java) { SourceId(" ") }
        assertThrows(IllegalArgumentException::class.java) { SourceAssetKey("") }
        assertThrows(IllegalArgumentException::class.java) { AssetLocation(" ") }
        assertThrows(IllegalArgumentException::class.java) { AssetFingerprint("") }
    }
}
