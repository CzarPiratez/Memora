package com.memora.app.application.find

import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.memora.app.application.documents.PdfPagePreviewRenderResult
import com.memora.app.application.documents.PdfPagePreviewRenderer
import com.memora.app.application.documents.PdfReadOnlyDescriptorAccess
import com.memora.app.application.documents.PdfReadOnlyDescriptorOutcome
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAccessModel
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceCapability
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import com.memora.app.domain.discovery.SourceAccessState
import com.memora.app.domain.extraction.PdfExtractionRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadFindResultThumbnailTest {
    @Test
    fun notes_are_glyphs_without_reopening_an_original() = runTest {
        val images = RecordingImageLoader()
        val result = useCase(imageLoader = images)(
            FindThumbnailRequest(
                sourceId = "notes",
                sourceAssetKey = "page-1",
                assetType = AssetType.NOTE,
                label = "Mubasher",
                pageNumber = null,
            ),
        )
        assertEquals(FindThumbnailResult.Glyph(FindThumbnailGlyph.NOTE), result)
        assertEquals(0, images.calls)
    }

    @Test
    fun photos_without_library_access_do_not_decode() = runTest {
        val images = RecordingImageLoader()
        val result = useCase(
            imageLoader = images,
            accessScope = null,
        )(
            FindThumbnailRequest(
                sourceId = "media",
                sourceAssetKey = "img-1",
                assetType = AssetType.PHOTO,
                label = "IMG.jpg",
                pageNumber = null,
            ),
        )
        assertEquals(FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE), result)
        assertEquals(0, images.calls)
    }

    private fun useCase(
        imageLoader: ImageThumbnailLoader,
        accessScope: ImageLibraryAccessScope? = ImageLibraryAccessScope.FULL_LIBRARY,
    ) = LoadFindResultThumbnail(
        cache = MemoryFindThumbnailCache(),
        assetRepository = EmptyAssets(),
        imageLibraryDiscoverySource = FakeImages(accessScope),
        uriResolver = object : MediaStoreImageUriResolver {
            override fun candidates(storedUri: Uri, displayName: String) = listOf(storedUri)
        },
        imageThumbnailLoader = imageLoader,
        descriptorAccess = RejectingPdfAccess(),
        pagePreviewRenderer = RejectingPdfRenderer(),
    )

    private class RecordingImageLoader : ImageThumbnailLoader {
        var calls: Int = 0
        override fun load(uri: Uri, maxEdgePx: Int): FindThumbnailResult.Ready? {
            calls += 1
            return null
        }
    }

    private class EmptyAssets : AssetRepository {
        override suspend fun save(record: AssetIndexRecord) = Unit
        override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = null
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

    private class RejectingPdfAccess : PdfReadOnlyDescriptorAccess {
        override suspend fun <T> withReadOnlyDescriptor(
            request: PdfExtractionRequest,
            cancellationSignal: CancellationSignal?,
            consume: (ParcelFileDescriptor) -> T,
        ): PdfReadOnlyDescriptorOutcome<T> = PdfReadOnlyDescriptorOutcome.SourceUnavailable
    }

    private class RejectingPdfRenderer : PdfPagePreviewRenderer {
        override fun renderPage(
            descriptor: ParcelFileDescriptor,
            pageNumber: Int,
            documentLabel: String,
            maxEdgePx: Int,
        ): PdfPagePreviewRenderResult = PdfPagePreviewRenderResult.CouldNotOpen
    }
}
