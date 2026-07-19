package com.memora.app.application.discovery

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCursor
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryPageStore
import com.memora.app.domain.discovery.DiscoveryResult
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProcessDiscoveryResultTest {
    @Test
    fun savesAndReturnsASuccessfulPage() = runBlocking {
        val store = RecordingStore()
        val result = DiscoveryResult.Page(page())

        val processed = ProcessDiscoveryResult(PersistDiscoveryPage(store))(result)

        assertEquals(result, processed)
        assertEquals(result.value, store.savedPage)
    }

    @Test
    fun preservesAccessAndSourceFailureOutcomesWithoutSaving() = runBlocking {
        val outcomes = listOf(
            DiscoveryResult.AccessRequired,
            DiscoveryResult.AccessRevoked,
            DiscoveryResult.Failed(DiscoveryFailure("source_unavailable", "The source is unavailable.")),
        )

        outcomes.forEach { outcome ->
            val store = RecordingStore()

            assertEquals(outcome, ProcessDiscoveryResult(PersistDiscoveryPage(store))(outcome))
            assertNull(store.savedPage)
        }
    }

    @Test
    fun returnsASafeFailureWhenAtomicStorageFails() = runBlocking {
        val result = DiscoveryResult.Page(page())

        val processed = ProcessDiscoveryResult(PersistDiscoveryPage(FailingStore()))(result)

        assertEquals(
            DiscoveryResult.Failed(
                DiscoveryFailure(
                    "discovery_page_persistence_failed",
                    "Memora could not safely save this discovery page. Please try again.",
                ),
            ),
            processed,
        )
    }

    private class RecordingStore : DiscoveryPageStore {
        var savedPage: DiscoveryPage? = null

        override suspend fun save(page: DiscoveryPage) {
            savedPage = page
        }
    }

    private class FailingStore : DiscoveryPageStore {
        override suspend fun save(page: DiscoveryPage): Nothing = error("database unavailable")
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
