package com.memora.app.data.mediastore

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Instant

/** Platform-neutral projection of the metadata read from one MediaStore image row. */
data class MediaStoreImageRow(
    val volumeName: String,
    val mediaId: Long,
    val displayName: String?,
    val relativePath: String?,
    val sizeBytes: Long,
    val generationModified: Long,
    val dateModifiedSeconds: Long,
) {
    init {
        require(volumeName.isNotBlank()) { "A MediaStore volume name cannot be blank." }
        require(mediaId >= 0) { "A MediaStore ID cannot be negative." }
        require(sizeBytes >= 0) { "A MediaStore image size cannot be negative." }
        require(generationModified >= 0) { "A MediaStore generation cannot be negative." }
        require(dateModifiedSeconds >= 0) { "A MediaStore modified time cannot be negative." }
    }
}

/** Maps metadata only; it never opens an image URI or reads an image's bytes. */
object MediaStoreImageMapper {
    val sourceId: SourceId = SourceId("android-media-store-images")

    fun toAsset(row: MediaStoreImageRow, discoveredAt: Instant): Asset {
        val assetKey = "${row.volumeName}:${row.mediaId}"
        return Asset(
            identity = AssetIdentity(sourceId, SourceAssetKey(assetKey)),
            type = if (isScreenshot(row)) AssetType.SCREENSHOT else AssetType.PHOTO,
            location = AssetLocation("content://media/${row.volumeName}/images/media/${row.mediaId}"),
            fingerprint = AssetFingerprint("$assetKey:${row.generationModified}:${row.sizeBytes}"),
            discoveredAt = discoveredAt,
            displayName = row.displayName?.takeIf(String::isNotBlank),
            sourceModifiedAt = Instant.ofEpochSecond(row.dateModifiedSeconds),
        )
    }

    private fun isScreenshot(row: MediaStoreImageRow): Boolean = sequenceOf(
        row.relativePath,
        row.displayName,
    ).filterNotNull().any { it.contains("screenshot", ignoreCase = true) }
}
