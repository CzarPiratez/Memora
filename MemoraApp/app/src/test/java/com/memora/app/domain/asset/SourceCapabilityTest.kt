package com.memora.app.domain.asset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceCapabilityTest {
    @Test
    fun `source capability records source access and supported asset types`() {
        val capability = SourceCapability(
            sourceId = SourceId("android-media-store"),
            supportedAssetTypes = setOf(AssetType.PHOTO, AssetType.SCREENSHOT),
            accessModel = SourceAccessModel.ANDROID_MEDIA_PERMISSION,
            supportsIncrementalDiscovery = true,
            supportsBackgroundIndexing = true,
        )

        assertEquals(SourceAccessModel.ANDROID_MEDIA_PERMISSION, capability.accessModel)
        assertEquals(setOf(AssetType.PHOTO, AssetType.SCREENSHOT), capability.supportedAssetTypes)
    }

    @Test
    fun `source capability rejects unsafe or incomplete source definitions`() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceCapability(
                sourceId = SourceId("invalid"),
                supportedAssetTypes = emptySet(),
                accessModel = SourceAccessModel.ANDROID_MEDIA_PERMISSION,
                supportsIncrementalDiscovery = true,
                supportsBackgroundIndexing = true,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            SourceCapability(
                sourceId = SourceId("write-capable"),
                supportedAssetTypes = setOf(AssetType.PDF),
                accessModel = SourceAccessModel.PERSISTED_DOCUMENT_ACCESS,
                supportsIncrementalDiscovery = true,
                supportsBackgroundIndexing = true,
                isReadOnly = false,
            )
        }
    }
}
