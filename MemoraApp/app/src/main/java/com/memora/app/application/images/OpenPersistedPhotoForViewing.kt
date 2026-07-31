package com.memora.app.application.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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

@Singleton
class OpenPersistedPhotoForViewing @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
) {
    suspend operator fun invoke(
        sourceId: String,
        sourceAssetKey: String,
        photoLabel: String,
    ): PhotoPreviewRenderResult = withContext(Dispatchers.IO) {
        require(sourceId.isNotBlank() && sourceAssetKey.isNotBlank() && photoLabel.isNotBlank())
        if (mediaStoreImageAccess(context) == MediaStoreAccess.REQUIRED) {
            return@withContext PhotoPreviewRenderResult.SourceUnavailable
        }
        val asset = assetRepository.find(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        )?.asset ?: return@withContext PhotoPreviewRenderResult.SourceUnavailable
        if (asset.type != AssetType.PHOTO) return@withContext PhotoPreviewRenderResult.CouldNotOpen
        val uri = try {
            Uri.parse(asset.location.value)
        } catch (_: Exception) {
            return@withContext PhotoPreviewRenderResult.CouldNotOpen
        }
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (!decode(uri, bounds) || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext PhotoPreviewRenderResult.CouldNotOpen
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = computeInSampleSize(
                    bounds.outWidth,
                    bounds.outHeight,
                    MAX_PREVIEW_EDGE_PX,
                )
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = decodeBitmap(uri, options)
                ?: return@withContext PhotoPreviewRenderResult.SourceUnavailable
            try {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                PhotoPreviewRenderResult.Ready(
                    photoLabel,
                    bitmap.width,
                    bitmap.height,
                    pixels,
                )
            } finally {
                bitmap.recycle()
            }
        } catch (_: SecurityException) {
            PhotoPreviewRenderResult.SourceUnavailable
        } catch (_: Exception) {
            PhotoPreviewRenderResult.CouldNotOpen
        }
    }

    private fun decode(uri: Uri, options: BitmapFactory.Options): Boolean =
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, options)
            true
        } == true

    private fun decodeBitmap(uri: Uri, options: BitmapFactory.Options): Bitmap? =
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, options)
        }

    companion object {
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
