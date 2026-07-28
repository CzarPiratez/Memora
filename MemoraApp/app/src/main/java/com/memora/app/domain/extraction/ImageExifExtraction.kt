package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType

/** Schema id for deterministic MediaStore image EXIF facts (not OCR). */
@JvmInline
value class ImageExifSchemaVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "Image EXIF schema version cannot be blank." }
    }

    companion object {
        val V1 = ImageExifSchemaVersion("image-exif-v1")
    }
}

/**
 * One durable EXIF fact record for a discovered PHOTO or SCREENSHOT.
 *
 * Does not claim OCR text, keyword search, or Memory ranking.
 */
data class ImageExifExtractionRecord(
    val asset: Asset,
    val schemaVersion: ImageExifSchemaVersion,
    val assetKind: AssetType,
    val datetimeOriginal: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val orientation: Int?,
    val make: String?,
    val model: String?,
    val extractedAtEpochMillis: Long,
    val integrity: String = INTEGRITY_VERIFIED,
) {
    init {
        require(assetKind == AssetType.PHOTO || assetKind == AssetType.SCREENSHOT) {
            "Image EXIF extract only applies to PHOTO or SCREENSHOT."
        }
        require(asset.type == assetKind) {
            "Asset type must match the stored image kind."
        }
        require(integrity.isNotBlank())
        require(imageWidth == null || imageWidth > 0)
        require(imageHeight == null || imageHeight > 0)
    }

    companion object {
        const val INTEGRITY_VERIFIED = "VERIFIED"
    }
}

/** Outcome of opening a permitted image for EXIF facts. */
sealed interface ImageExifReadResult {
    data class Facts(
        val datetimeOriginal: String?,
        val imageWidth: Int?,
        val imageHeight: Int?,
        val orientation: Int?,
        val make: String?,
        val model: String?,
    ) : ImageExifReadResult

    data object AccessStopped : ImageExifReadResult

    data object RetryableFailure : ImageExifReadResult
}
