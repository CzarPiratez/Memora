package com.memora.app.application.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.memora.app.application.find.MediaStoreImageUriResolver
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a read-only in-app preview of the original screenshot for a keyword hit.
 *
 * Reopens MediaStore only after the user taps Open. Search itself stays on stored
 * Room OCR text. Does not edit, copy, or claim ownership of the file.
 *
 * Tries the stored URI, alternate volume URI forms, then display-name resolve —
 * MediaStore IDs/volumes can shift after emulator cold boot while OCR rows remain.
 */
@Singleton
class OpenPersistedScreenshotForViewing @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
    private val imageLibraryDiscoverySource: ImageLibraryDiscoverySource,
    private val uriResolver: MediaStoreImageUriResolver,
) {
    suspend operator fun invoke(
        sourceId: String,
        sourceAssetKey: String,
        screenshotLabel: String,
    ): ScreenshotPreviewRenderResult = withContext(Dispatchers.IO) {
        require(sourceId.isNotBlank()) { "Open original needs a source id." }
        require(sourceAssetKey.isNotBlank()) { "Open original needs a source asset key." }
        require(screenshotLabel.isNotBlank()) { "Open original needs a screenshot label." }

        if (imageLibraryDiscoverySource.accessScope() == null) {
            Log.w(TAG, "Open blocked: photo access required.")
            return@withContext ScreenshotPreviewRenderResult.SourceUnavailable
        }

        val record = assetRepository.find(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        )
        if (record == null) {
            Log.w(TAG, "Open blocked: asset not found for $sourceId / $sourceAssetKey")
            return@withContext ScreenshotPreviewRenderResult.SourceUnavailable
        }

        val asset = record.asset
        if (asset.type != AssetType.SCREENSHOT) {
            Log.w(TAG, "Open blocked: asset type ${asset.type}")
            return@withContext ScreenshotPreviewRenderResult.CouldNotOpen
        }

        val storedUri = try {
            Uri.parse(asset.location.value)
        } catch (_: Exception) {
            return@withContext ScreenshotPreviewRenderResult.CouldNotOpen
        }

        val displayName = asset.displayName?.takeIf { it.isNotBlank() } ?: screenshotLabel
        val candidates = uriResolver.candidates(storedUri = storedUri, displayName = displayName)
        Log.i(TAG, "Open trying ${candidates.size} URI candidate(s) for $displayName")

        var sawSecurity = false
        var sawHardFailure = false
        for (uri in candidates) {
            try {
                when (val decoded = decodeScaledPreview(uri = uri, screenshotLabel = screenshotLabel)) {
                    is ScreenshotPreviewRenderResult.Ready -> return@withContext decoded
                    ScreenshotPreviewRenderResult.SourceUnavailable -> Unit
                    ScreenshotPreviewRenderResult.CouldNotOpen -> sawHardFailure = true
                }
            } catch (_: SecurityException) {
                sawSecurity = true
                Log.w(TAG, "SecurityException opening $uri")
            } catch (error: Exception) {
                sawHardFailure = true
                Log.w(TAG, "Open failed for $uri: ${error.javaClass.simpleName}")
            }
        }

        return@withContext when {
            sawSecurity -> ScreenshotPreviewRenderResult.SourceUnavailable
            sawHardFailure -> ScreenshotPreviewRenderResult.CouldNotOpen
            else -> ScreenshotPreviewRenderResult.SourceUnavailable
        }
    }

    private fun decodeScaledPreview(
        uri: Uri,
        screenshotLabel: String,
    ): ScreenshotPreviewRenderResult {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (!decodeInto(uri, bounds)) {
            return ScreenshotPreviewRenderResult.SourceUnavailable
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return ScreenshotPreviewRenderResult.CouldNotOpen
        }

        val sampleSize = computeInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxEdgePx = MAX_PREVIEW_EDGE_PX,
        )
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = decodeBitmap(uri, decodeOptions)
            ?: return ScreenshotPreviewRenderResult.SourceUnavailable

        return try {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) {
                return ScreenshotPreviewRenderResult.CouldNotOpen
            }
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            ScreenshotPreviewRenderResult.Ready(
                screenshotLabel = screenshotLabel,
                widthPx = width,
                heightPx = height,
                argb8888 = pixels,
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeInto(uri: Uri, options: BitmapFactory.Options): Boolean {
        val resolver = context.applicationContext.contentResolver
        try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, options)
                return true
            }
        } catch (_: SecurityException) {
            throw SecurityException("openFileDescriptor denied for $uri")
        } catch (_: Exception) {
            // Fall through to stream open.
        }
        return try {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
                true
            } == true
        } catch (_: SecurityException) {
            throw SecurityException("openInputStream denied for $uri")
        } catch (_: Exception) {
            false
        }
    }

    private fun decodeBitmap(uri: Uri, options: BitmapFactory.Options): Bitmap? {
        val resolver = context.applicationContext.contentResolver
        try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd: ParcelFileDescriptor ->
                return BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, options)
            }
        } catch (_: SecurityException) {
            throw SecurityException("openFileDescriptor denied for $uri")
        } catch (_: Exception) {
            // Fall through.
        }
        return try {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: SecurityException) {
            throw SecurityException("openInputStream denied for $uri")
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "MemoraShotOpen"

        /** Hard cap so a 1080×2400 phone shot is sampled before UI display. */
        const val MAX_PREVIEW_EDGE_PX = 960

        fun computeInSampleSize(width: Int, height: Int, maxEdgePx: Int): Int {
            var sample = 1
            var w = width
            var h = height
            while (max(w, h) > maxEdgePx) {
                sample *= 2
                w /= 2
                h /= 2
            }
            return sample.coerceAtLeast(1)
        }
    }
}
