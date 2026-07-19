package com.memora.app.data.mediastore

import com.memora.app.domain.asset.AssetType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreImageMapperTest {
    @Test
    fun `maps image metadata to a stable read-only photo asset`() {
        val asset = MediaStoreImageMapper.toAsset(row(), Instant.parse("2026-07-18T10:00:00Z"))

        assertEquals("external_primary:42", asset.identity.sourceAssetKey.value)
        assertEquals("external_primary:42:9:2048", asset.fingerprint.value)
        assertEquals(AssetType.PHOTO, asset.type)
    }

    @Test
    fun `classifies screenshot from deterministic metadata only`() {
        val asset = MediaStoreImageMapper.toAsset(
            row(relativePath = "Pictures/Screenshots/"),
            Instant.parse("2026-07-18T10:00:00Z"),
        )

        assertEquals(AssetType.SCREENSHOT, asset.type)
    }

    private fun row(relativePath: String? = "DCIM/Camera/") = MediaStoreImageRow(
        volumeName = "external_primary",
        mediaId = 42,
        displayName = "lake.jpg",
        relativePath = relativePath,
        sizeBytes = 2048,
        generationModified = 9,
        dateModifiedSeconds = 1_752_000_000,
    )
}
