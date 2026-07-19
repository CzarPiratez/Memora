package com.memora.app.application.discovery

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCursor
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryPageStore
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistDiscoveryPageTest {
    @Test
    fun delegatesTheWholePageToTheAtomicStore() = runBlocking {
        val store = RecordingStore()
        val page = page()

        PersistDiscoveryPage(store)(page)

        assertEquals(page, store.savedPage)
    }

    private class RecordingStore : DiscoveryPageStore {
        var savedPage: DiscoveryPage? = null

        override suspend fun save(page: DiscoveryPage) {
            savedPage = page
        }
    }

    private fun page(): DiscoveryPage {
        val sourceId = SourceId("android-media-store-images")
        return DiscoveryPage(
            sourceId = sourceId,
            assets = listOf(
                Asset(
                    identity = AssetIdentity(sourceId, SourceAssetKey("external_primary:42")),
                    type = AssetType.PHOTO,
                    location = AssetLocation("content://media/external_primary/images/media/42"),
                    fingerprint = AssetFingerprint("external_primary:42:9:2048"),
                    discoveredAt = Instant.parse("2026-07-19T10:00:00Z"),
                ),
            ),
            checkpoint = DiscoveryCursor(sourceId, "opaque-v1"),
            hasMore = false,
        )
    }
}
