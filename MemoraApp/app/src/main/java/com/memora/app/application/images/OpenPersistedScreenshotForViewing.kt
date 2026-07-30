package com.memora.app.application.images

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.memora.app.data.mediastore.MediaStoreAccess
import com.memora.app.data.mediastore.mediaStoreImageAccess
import com.memora.app.domain.asset.AssetIdentity
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
 * Decode is hard-capped. If the stored content URI is stale (common after a
 * MediaStore rescan), falls back to resolving the current URI by display name.
 */
@Singleton
class OpenPersistedScreenshotForViewing @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
) {
    suspend operator fun invoke(
        sourceId: String,
        sourceAssetKey: String,
        screenshotLabel: String,
    ): ScreenshotPreviewRenderResult = withContext(Dispatchers.IO) {
        require(sourceId.isNotBlank()) { "Open original needs a source id." }
        require(sourceAssetKey.isNotBlank()) { "Open original needs a source asset key." }
        require(screenshotLabel.isNotBlank()) { "Open original needs a screenshot label." }

        if (mediaStoreImageAccess(context) == MediaStoreAccess.REQUIRED) {
            return@withContext ScreenshotPreviewRenderResult.SourceUnavailable
        }

        val record = assetRepository.find(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        ) ?: return@withContext ScreenshotPreviewRenderResult.SourceUnavailable

        val asset = record.asset
        if (asset.type != AssetType.SCREENSHOT) {
            return@withContext ScreenshotPreviewRenderResult.CouldNotOpen
        }

        val storedUri = try {
            Uri.parse(asset.location.value)
        } catch (_: Exception) {
            return@withContext ScreenshotPreviewRenderResult.CouldNotOpen
        }

        val displayName = asset.displayName?.takeIf { it.isNotBlank() } ?: screenshotLabel

        try {
            val openableUri = when {
                canOpen(storedUri) -> storedUri
                else -> resolveCurrentUriByDisplayName(displayName)
                    ?: return@withContext ScreenshotPreviewRenderResult.SourceUnavailable
            }
            decodeScaledPreview(uri = openableUri, screenshotLabel = screenshotLabel)
        } catch (_: SecurityException) {
            ScreenshotPreviewRenderResult.SourceUnavailable
        } catch (_: Exception) {
            ScreenshotPreviewRenderResult.CouldNotOpen
        }
    }

    private fun canOpen(uri: Uri): Boolean {
        return try {
            context.applicationContext.contentResolver.openInputStream(uri)?.use { true } == true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveCurrentUriByDisplayName(displayName: String): Uri? {
        val resolver = context.applicationContext.contentResolver
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        return resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(displayName),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val id = cursor.getLong(0)
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        }
    }

    private fun decodeScaledPreview(
        uri: Uri,
        screenshotLabel: String,
    ): ScreenshotPreviewRenderResult {
        val resolver = context.applicationContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return ScreenshotPreviewRenderResult.SourceUnavailable

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
        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return ScreenshotPreviewRenderResult.SourceUnavailable

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

    companion object {
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
