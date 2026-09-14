package com.memora.app.application.share

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.IndexingState
import com.memora.app.domain.asset.SourceAccessModel
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceCapability
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import com.memora.app.domain.discovery.SourceAccessState
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PrepareShareOriginalTest {
    @Test
    fun photo_without_library_access_is_unavailable() = runTest {
        val result = useCase(access = null)(
            ShareOriginalRequest.Photo("media", "p1", "receipt.jpg"),
        )
        assertEquals(PreparedShareOriginal.SourceUnavailable, result)
    }

    @Test
    fun screenshot_uses_first_readable_candidate() = runTest {
        val stored = "content://media/external/images/media/1"
        val live = "content://media/external/images/media/99"
        val result = useCase(
            asset = imageAsset(AssetType.SCREENSHOT, stored),
            candidates = listOf(stored, live),
            readable = setOf(live),
        )(
            ShareOriginalRequest.Screenshot("media", "p1", "shot.png"),
        )
        val ready = result as PreparedShareOriginal.Ready
        assertEquals(live, ready.uri)
        assertEquals(ShareOriginalMime.SCREENSHOT_FALLBACK, ready.mimeType)
    }

    @Test
    fun pdf_ready_uses_canonical_uri_not_asset_location() = runTest {
        val result = useCase(
            asset = pdfAsset("content://untrusted/document/old"),
            pdfUri = ShareablePdfUri.Ready("content://tree/document/canonical"),
        )(
            ShareOriginalRequest.Pdf("saf", "doc-1", "pool.pdf"),
        )
        val ready = result as PreparedShareOriginal.Ready
        assertEquals("content://tree/document/canonical", ready.uri)
        assertEquals(ShareOriginalMime.PDF, ready.mimeType)
        assertEquals("pool.pdf", ready.label)
    }

    @Test
    fun missing_asset_is_unavailable() = runTest {
        val result = useCase(asset = null)(
            ShareOriginalRequest.Photo("media", "missing", "gone.jpg"),
        )
        assertEquals(PreparedShareOriginal.SourceUnavailable, result)
    }

    @Test
    fun type_mismatch_cannot_share() = runTest {
        val result = useCase(
            asset = imageAsset(AssetType.PHOTO, "content://media/1"),
        )(
            ShareOriginalRequest.Screenshot("media", "p1", "shot.png"),
        )
        assertEquals(PreparedShareOriginal.CouldNotShare, result)
    }

    private fun useCase(
        asset: Asset? = imageAsset(AssetType.PHOTO, "content://media/1"),
        access: ImageLibraryAccessScope? = ImageLibraryAccessScope.FULL_LIBRARY,
        candidates: List<String> = listOf("content://media/1"),
        readable: Set<String> = setOf("content://media/1"),
        pdfUri: ShareablePdfUri = ShareablePdfUri.CouldNotShare,
    ) = PrepareShareOriginal(
        assetRepository = OneAssetRepository(asset),
        imageLibraryDiscoverySource = FakeImages(access),
        uriCandidates = ShareImageUriCandidates { _, _ -> candidates },
        readable = object : ReadableContentUri {
            override fun canRead(uri: String) = uri in readable
            override fun mimeType(uri: String, fallback: String) = fallback
        },
        pdfUriAccess = object : ShareablePdfUriAccess {
            override suspend fun resolve(sourceId: String, sourceAssetKey: String) = pdfUri
        },
    )

    private fun imageAsset(type: AssetType, location: String) = Asset(
        identity = AssetIdentity(SourceId("media"), SourceAssetKey("p1")),
        type = type,
        location = AssetLocation(location),
        fingerprint = AssetFingerprint("fp"),
        discoveredAt = Instant.EPOCH,
        displayName = "shot.png",
    )

    private fun pdfAsset(location: String) = Asset(
        identity = AssetIdentity(SourceId("saf"), SourceAssetKey("doc-1")),
        type = AssetType.PDF,
        location = AssetLocation(location),
        fingerprint = AssetFingerprint("fp"),
        discoveredAt = Instant.EPOCH,
        displayName = "pool.pdf",
    )

    private class OneAssetRepository(
        private val asset: Asset?,
    ) : AssetRepository {
        override suspend fun save(record: AssetIndexRecord) = Unit
        override suspend fun find(identity: AssetIdentity): AssetIndexRecord? =
            asset?.takeIf { it.identity == identity }?.let {
                AssetIndexRecord(it, IndexingState.discovered)
            }
        override suspend fun findFirstBySourceAndType(sourceId: SourceId, type: AssetType): Asset? = null
        override suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int = 0
        override suspend fun findNextPdfPendingLocalReading(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
        override suspend fun countPdfPendingLocalReading(sourceId: SourceId, schemaVersion: String): Int = 0
        override suspend fun findNextImagePendingExifExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
        override suspend fun findNextScreenshotPendingOcrExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
        override suspend fun findNextPhotoPendingOcrExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
        override suspend fun findNextNotePendingPageExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
    }

    private class FakeImages(
        private val scope: ImageLibraryAccessScope?,
    ) : ImageLibraryDiscoverySource {
        override val capability = SourceCapability(
            sourceId = SourceId("mediastore.images"),
            supportedAssetTypes = setOf(AssetType.PHOTO, AssetType.SCREENSHOT),
            accessModel = SourceAccessModel.ANDROID_MEDIA_PERMISSION,
            supportsIncrementalDiscovery = true,
            supportsBackgroundIndexing = true,
        )
        override suspend fun accessState() = SourceAccessState.GRANTED
        override suspend fun discover(request: DiscoveryRequest) = DiscoveryResult.AccessRequired
        override suspend fun accessScope() = scope
    }
}
