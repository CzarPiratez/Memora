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

class ScreenshotOcrExtractionTest {
    @Test
    fun acceptsEmptyTextAsCompletedExtract() {
        val record = ScreenshotOcrExtractionRecord(
            asset = screenshotAsset(),
            schemaVersion = ScreenshotOcrSchemaVersion.V1,
            fullText = "",
            textTruncated = false,
            engineId = ScreenshotOcrExtractionRecord.ENGINE_MLKIT_LATIN_BUNDLED,
            engineVersion = "16.0.1",
            extractedAtEpochMillis = 1L,
        )
        assertEquals("", record.fullText)
        assertEquals(false, record.textTruncated)
    }

    @Test
    fun rejectsOversizedStoredText() {
        val oversized = "a".repeat(ScreenshotOcrExtractionRecord.MAX_STORED_CHARS + 1)
        val failed = runCatching {
            ScreenshotOcrExtractionRecord(
                asset = screenshotAsset(),
                schemaVersion = ScreenshotOcrSchemaVersion.V1,
                fullText = oversized,
                textTruncated = true,
                engineId = ScreenshotOcrExtractionRecord.ENGINE_MLKIT_LATIN_BUNDLED,
                engineVersion = "16.0.1",
                extractedAtEpochMillis = 1L,
            )
        }
        assertTrue(failed.isFailure)
    }

    private fun screenshotAsset(): Asset = Asset(
        identity = AssetIdentity(
            sourceId = SourceId("android-media-store-images"),
            sourceAssetKey = SourceAssetKey("external_primary:1"),
        ),
        type = AssetType.SCREENSHOT,
        location = AssetLocation("content://media/external_primary/images/media/1"),
        fingerprint = AssetFingerprint("external_primary:1:1:1"),
        discoveredAt = Instant.EPOCH,
        displayName = "Screenshot_memora_note.png",
        sourceModifiedAt = Instant.EPOCH,
    )
}
