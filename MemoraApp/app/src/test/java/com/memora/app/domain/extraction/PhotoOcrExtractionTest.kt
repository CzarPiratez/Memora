package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoOcrExtractionTest {
    @Test
    fun acceptsPhotoAndEmptyCompletedText() {
        val record = record(photo())
        assertEquals(AssetType.PHOTO, record.asset.type)
        assertEquals("", record.fullText)
    }

    @Test
    fun rejectsScreenshot() {
        assertTrue(runCatching { record(photo().copy(type = AssetType.SCREENSHOT)) }.isFailure)
    }

    private fun record(asset: Asset) = PhotoOcrExtractionRecord(
        asset = asset,
        schemaVersion = PhotoOcrSchemaVersion.V1,
        fullText = "",
        textTruncated = false,
        engineId = PhotoOcrExtractionRecord.ENGINE_MLKIT_LATIN_BUNDLED,
        engineVersion = "16.0.1",
        extractedAtEpochMillis = 1L,
    )

    private fun photo() = Asset(
        identity = AssetIdentity(SourceId("android-media-store-images"), SourceAssetKey("photo-1")),
        type = AssetType.PHOTO,
        location = AssetLocation("content://media/photo/1"),
        fingerprint = AssetFingerprint("photo-fingerprint"),
        discoveredAt = Instant.EPOCH,
        displayName = "receipt.jpg",
        sourceModifiedAt = Instant.EPOCH,
    )
}
