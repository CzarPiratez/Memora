package com.memora.app.data.mediastore

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.extraction.ImageExifReadResult
import com.memora.app.domain.extraction.ImageExifReader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens a discovered MediaStore image URI read-only and reads EXIF tags.
 *
 * Discovery must not call this (ADR-009). Extract path only. No OCR / network.
 */
@Singleton
class ContentResolverImageExifReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ImageExifReader {
    override fun read(asset: Asset): ImageExifReadResult {
        val uri = try {
            Uri.parse(asset.location.value)
        } catch (_: Exception) {
            return ImageExifReadResult.RetryableFailure
        }

        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                ImageExifReadResult.Facts(
                    datetimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
                    imageWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1)
                        .takeIf { it > 0 },
                    imageHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1)
                        .takeIf { it > 0 },
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED,
                    ).takeIf { it != ExifInterface.ORIENTATION_UNDEFINED },
                    make = exif.getAttribute(ExifInterface.TAG_MAKE)?.takeIf { it.isNotBlank() },
                    model = exif.getAttribute(ExifInterface.TAG_MODEL)?.takeIf { it.isNotBlank() },
                )
            } ?: ImageExifReadResult.AccessStopped
        } catch (_: SecurityException) {
            ImageExifReadResult.AccessStopped
        } catch (_: IOException) {
            ImageExifReadResult.RetryableFailure
        } catch (_: Exception) {
            ImageExifReadResult.RetryableFailure
        }
    }
}
