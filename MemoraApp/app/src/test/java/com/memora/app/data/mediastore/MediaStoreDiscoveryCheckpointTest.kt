package com.memora.app.data.mediastore

import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCursor
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreDiscoveryCheckpointTest {
    @Test
    fun `round trips an opaque MediaStore checkpoint`() {
        val checkpoint = MediaStoreDiscoveryCheckpoint(
            storeVersion = "media-store-version:2026-07-19",
            watermark = 91,
            lastMediaId = 42,
        )

        val restored = MediaStoreDiscoveryCheckpoint.from(checkpoint.toCursor())

        assertEquals(checkpoint, restored)
        assertEquals(MediaStoreImageMapper.sourceId, checkpoint.toCursor().sourceId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a checkpoint from another source`() {
        MediaStoreDiscoveryCheckpoint.from(
            DiscoveryCursor(SourceId("another-source"), "v1-bGVnYWN5:0:0")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a malformed checkpoint`() {
        MediaStoreDiscoveryCheckpoint.from(
            DiscoveryCursor(MediaStoreImageMapper.sourceId, "not-a-mediastore-checkpoint")
        )
    }
}
