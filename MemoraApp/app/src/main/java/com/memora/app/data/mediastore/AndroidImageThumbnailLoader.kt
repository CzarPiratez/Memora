package com.memora.app.data.mediastore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.util.Size
import com.memora.app.application.find.ImageThumbnailLoad
import com.memora.app.application.find.ImageThumbnailLoader
import com.memora.app.application.images.OpenPersistedPhotoForViewing
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class AndroidImageThumbnailLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ImageThumbnailLoader {
    override fun load(location: String, maxEdgePx: Int): ImageThumbnailLoad {
        require(maxEdgePx > 0)
        require(location.isNotBlank())
        val uri = try {
            Uri.parse(location)
        } catch (_: Exception) {
            return ImageThumbnailLoad.Unreachable
        } ?: return ImageThumbnailLoad.Unreachable
        val bitmap = try {
            decode(uri, maxEdgePx)
        } catch (_: SecurityException) {
            return ImageThumbnailLoad.Unreachable
        } catch (_: FileNotFoundException) {
            return ImageThumbnailLoad.Unreachable
        } catch (_: Exception) {
            return ImageThumbnailLoad.CouldNotDecode
        } ?: return classifyNullDecode(uri)
        return try {
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                ImageThumbnailLoad.CouldNotDecode
            } else {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                ImageThumbnailLoad.Ready(
                    widthPx = bitmap.width,
                    heightPx = bitmap.height,
                    argb8888 = pixels,
                )
            }
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * [decode] returned null without throwing. Open the descriptor once more
     * so a missing file is Unreachable and a corrupt/undecodable file is not.
     */
    private fun classifyNullDecode(uri: Uri): ImageThumbnailLoad =
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                ImageThumbnailLoad.CouldNotDecode
            } ?: ImageThumbnailLoad.Unreachable
        } catch (_: SecurityException) {
            ImageThumbnailLoad.Unreachable
        } catch (_: FileNotFoundException) {
            ImageThumbnailLoad.Unreachable
        } catch (_: Exception) {
            ImageThumbnailLoad.CouldNotDecode
        }

    private fun decode(uri: Uri, maxEdgePx: Int): Bitmap? {
        val resolver = context.applicationContext.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val loaded = resolver.loadThumbnail(uri, Size(maxEdgePx, maxEdgePx), CancellationSignal())
            return fitToEdge(loaded, maxEdgePx)
        }
        return decodeSampled(uri, maxEdgePx)
    }

    private fun decodeSampled(uri: Uri, maxEdgePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (!decodeInto(uri, bounds) || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = OpenPersistedPhotoForViewing.computeInSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                maxEdgePx,
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = decodeBitmap(uri, options) ?: return null
        return fitToEdge(decoded, maxEdgePx)
    }

    private fun fitToEdge(bitmap: Bitmap, maxEdgePx: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdgePx) return bitmap
        val scale = maxEdgePx.toFloat() / longest.toFloat()
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun decodeInto(uri: Uri, options: BitmapFactory.Options): Boolean =
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, options)
            true
        } == true

    private fun decodeBitmap(uri: Uri, options: BitmapFactory.Options): Bitmap? =
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, options)
        }
}
