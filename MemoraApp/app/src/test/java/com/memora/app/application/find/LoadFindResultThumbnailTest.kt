package com.memora.app.application.find

import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.memora.app.application.documents.PdfPagePreviewRenderResult
import com.memora.app.application.documents.PdfPagePreviewRenderer
import com.memora.app.application.documents.PdfReadOnlyDescriptorAccess
import com.memora.app.application.documents.PdfReadOnlyDescriptorOutcome
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

    @Test
    fun missing_photo_is_a_gone_original_not_a_decode_flake() = runTest {
        val images = RecordingImageLoader()
        val result = useCase(imageLoader = images)(
            FindThumbnailRequest(
                sourceId = "media",
                sourceAssetKey = "img-1",
                assetType = AssetType.PHOTO,
                label = "IMG.jpg",
                pageNumber = null,
            ),
        )
        assertEquals(
            FindThumbnailResult.Glyph(FindThumbnailGlyph.SOURCE_UNREACHABLE),
            result,
        )
        assertEquals(0, images.calls)
    }

    @Test
    fun photo_uri_unreachable_is_a_gone_original() = runTest {
        val images = RecordingImageLoader(ImageThumbnailLoad.Unreachable)
        val result = useCase(
            imageLoader = images,
            assets = EmptyAssets(photoRecord()),
        )(
            FindThumbnailRequest(
                sourceId = "media",
                sourceAssetKey = "img-1",
                assetType = AssetType.PHOTO,
                label = "IMG.jpg",
                pageNumber = null,
            ),
        )
        assertEquals(
            FindThumbnailResult.Glyph(FindThumbnailGlyph.SOURCE_UNREACHABLE),
            result,
        )
        assertEquals(1, images.calls)
    }

    @Test
    fun screenshot_decode_flake_does_not_claim_the_file_is_gone() = runTest {
        val images = RecordingImageLoader(ImageThumbnailLoad.CouldNotDecode)
        val result = useCase(
            imageLoader = images,
            assets = EmptyAssets(screenshotRecord()),
        )(
            FindThumbnailRequest(
                sourceId = "media",
                sourceAssetKey = "shot-1",
                assetType = AssetType.SCREENSHOT,
                label = "Screenshot.png",
                pageNumber = null,
            ),
        )
        assertEquals(
            FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE),
            result,
        )
        assertEquals(1, images.calls)
    }

    @Test
    fun later_image_uri_candidate_can_still_decode() = runTest {
        val pixels = intArrayOf(0xFFFFFFFF.toInt())
        val images = RecordingImageLoader(
            ImageThumbnailLoad.Unreachable,
            ImageThumbnailLoad.Ready(1, 1, pixels),
        )
        val result = useCase(
            imageLoader = images,
            assets = EmptyAssets(photoRecord()),
            uriResolver = twoLocationResolver(),
        )(
            FindThumbnailRequest(
                sourceId = "media",
                sourceAssetKey = "img-1",
                assetType = AssetType.PHOTO,
                label = "IMG.jpg",
                pageNumber = null,
            ),
        )
        assertEquals(FindThumbnailResult.Ready(1, 1, pixels), result)
        assertEquals(2, images.calls)
    }

    @Test
    fun image_decode_flake_on_any_candidate_does_not_claim_gone() = runTest {
        val images = RecordingImageLoader(
            ImageThumbnailLoad.Unreachable,
            ImageThumbnailLoad.CouldNotDecode,
        )
        val result = useCase(
            imageLoader = images,
            assets = EmptyAssets(photoRecord()),
            uriResolver = twoLocationResolver(),
        )(
            FindThumbnailRequest(
                sourceId = "media",
                sourceAssetKey = "img-1",
                assetType = AssetType.PHOTO,
                label = "IMG.jpg",
                pageNumber = null,
            ),
        )
        assertEquals(
            FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE),
            result,
        )
        assertEquals(2, images.calls)
    }

    @Test
    fun empty_image_candidates_are_a_flake_not_a_gone_original() = runTest {
        val images = RecordingImageLoader(ImageThumbnailLoad.Unreachable)
        val result = useCase(
            imageLoader = images,
            assets = EmptyAssets(photoRecord()),
            uriResolver = object : MediaStoreImageUriResolver {
                override fun candidates(storedUri: Uri, displayName: String) = emptyList<Uri>()
                override fun candidateLocations(storedLocation: String, displayName: String) =
                    emptyList<String>()
            },
        )(
            FindThumbnailRequest(
                sourceId = "media",
                sourceAssetKey = "img-1",
                assetType = AssetType.PHOTO,
                label = "IMG.jpg",
                pageNumber = null,
            ),
        )
        assertEquals(
            FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE),
            result,
        )
        assertEquals(0, images.calls)
    }

    @Test
    fun missing_pdf_is_a_gone_original_not_a_decode_flake() = runTest {
        val result = useCase()(
            FindThumbnailRequest(
                sourceId = "saf",
                sourceAssetKey = "doc-1",
                assetType = AssetType.PDF,
                label = "gone.pdf",
                pageNumber = 1,
            ),
        )
        assertEquals(
            FindThumbnailResult.Glyph(FindThumbnailGlyph.SOURCE_UNREACHABLE),
            result,
        )
    }

    @Test
    fun pdf_descriptor_source_unavailable_is_unreachable() = runTest {
        val result = useCase(assets = EmptyAssets(pdfRecord()))(
            FindThumbnailRequest(
                sourceId = "saf",
                sourceAssetKey = "doc-1",
                assetType = AssetType.PDF,
                label = "gone.pdf",
                pageNumber = 1,
            ),
        )
        assertEquals(
            FindThumbnailResult.Glyph(FindThumbnailGlyph.SOURCE_UNREACHABLE),
            result,
        )
    }

    @Test
    fun pdf_render_flake_does_not_claim_the_file_is_gone() = runTest {
        val result = useCase(
            assets = EmptyAssets(pdfRecord()),
            descriptorAccess = FlakePdfAccess(),
        )(
            FindThumbnailRequest(
                sourceId = "saf",
                sourceAssetKey = "doc-1",
                assetType = AssetType.PDF,
                label = "gone.pdf",
                pageNumber = 1,
            ),
        )
        assertEquals(
            FindThumbnailResult.Glyph(FindThumbnailGlyph.UNAVAILABLE),
            result,
        )
    }

    private fun pdfRecord() = AssetIndexRecord(
        asset = Asset(
            identity = AssetIdentity(SourceId("saf"), SourceAssetKey("doc-1")),
            type = AssetType.PDF,
            location = AssetLocation("content://com.android.externalstorage.documents/tree/x/document/y"),
            fingerprint = AssetFingerprint("fp"),
            discoveredAt = java.time.Instant.EPOCH,
            displayName = "gone.pdf",
        ),
        indexingState = IndexingState.discovered,
    )

    private fun photoRecord() = imageRecord(
        key = "img-1",
        type = AssetType.PHOTO,
        name = "IMG.jpg",
        location = "content://media/external/images/media/1",
    )

    private fun screenshotRecord() = imageRecord(
        key = "shot-1",
        type = AssetType.SCREENSHOT,
        name = "Screenshot.png",
        location = "content://media/external/images/media/2",
    )

    private fun imageRecord(
        key: String,
        type: AssetType,
        name: String,
        location: String,
    ) = AssetIndexRecord(
        asset = Asset(
            identity = AssetIdentity(SourceId("media"), SourceAssetKey(key)),
            type = type,
            location = AssetLocation(location),
            fingerprint = AssetFingerprint("fp"),
            discoveredAt = java.time.Instant.EPOCH,
            displayName = name,
        ),
        indexingState = IndexingState.discovered,
    )

    private fun twoLocationResolver() = object : MediaStoreImageUriResolver {
        override fun candidates(storedUri: Uri, displayName: String) = emptyList<Uri>()
        override fun candidateLocations(storedLocation: String, displayName: String) = listOf(
            storedLocation,
            "content://media/external/images/media/9",
        )
    }

    private fun useCase(
        imageLoader: ImageThumbnailLoader = RecordingImageLoader(),
        accessScope: ImageLibraryAccessScope? = ImageLibraryAccessScope.FULL_LIBRARY,
        assets: AssetRepository = EmptyAssets(),
        descriptorAccess: PdfReadOnlyDescriptorAccess = RejectingPdfAccess(),
        uriResolver: MediaStoreImageUriResolver = object : MediaStoreImageUriResolver {
            override fun candidates(storedUri: Uri, displayName: String) = emptyList<Uri>()
            override fun candidateLocations(storedLocation: String, displayName: String) =
                listOf(storedLocation)
        },
    ) = LoadFindResultThumbnail(
        cache = MemoryFindThumbnailCache(),
        assetRepository = assets,
        imageLibraryDiscoverySource = FakeImages(accessScope),
        uriResolver = uriResolver,
        imageThumbnailLoader = imageLoader,
        descriptorAccess = descriptorAccess,
        pagePreviewRenderer = RejectingPdfRenderer(),
    )

    private class RecordingImageLoader(
        private vararg val outcomes: ImageThumbnailLoad,
    ) : ImageThumbnailLoader {
        var calls: Int = 0
        override fun load(location: String, maxEdgePx: Int): ImageThumbnailLoad {
            val outcome = outcomes.getOrNull(calls) ?: ImageThumbnailLoad.Unreachable
            calls += 1
            return outcome
        }
    }

    private class EmptyAssets(
        private val record: AssetIndexRecord? = null,
    ) : AssetRepository {
        override suspend fun save(record: AssetIndexRecord) = Unit
        override suspend fun find(identity: AssetIdentity): AssetIndexRecord? =
            record?.takeIf { it.asset.identity == identity }
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

    private class FlakePdfAccess : PdfReadOnlyDescriptorAccess {
        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> withReadOnlyDescriptor(
            request: PdfExtractionRequest,
            cancellationSignal: CancellationSignal?,
            consume: (ParcelFileDescriptor) -> T,
        ): PdfReadOnlyDescriptorOutcome<T> =
            PdfReadOnlyDescriptorOutcome.Consumed(PdfPagePreviewRenderResult.CouldNotOpen as T)
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
