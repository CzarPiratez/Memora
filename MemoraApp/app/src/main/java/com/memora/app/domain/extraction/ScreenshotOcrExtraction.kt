package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType

/** Schema id for deterministic screenshot OCR text (not Memory / keyword search). */
@JvmInline
value class ScreenshotOcrSchemaVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "Screenshot OCR schema version cannot be blank." }
    }

    companion object {
        val V1 = ScreenshotOcrSchemaVersion("screenshot-ocr-v1")
    }
}

/**
 * One durable OCR text record for a discovered SCREENSHOT.
 *
 * Empty [fullText] is a valid completed extract (no readable text found).
 * Does not claim keyword search, Memory ranking, or PHOTO OCR.
 */
data class ScreenshotOcrExtractionRecord(
    val asset: Asset,
    val schemaVersion: ScreenshotOcrSchemaVersion,
    val fullText: String,
    val textTruncated: Boolean,
    val engineId: String,
    val engineVersion: String,
    val extractedAtEpochMillis: Long,
    val integrity: String = INTEGRITY_VERIFIED,
) {
    init {
        require(asset.type == AssetType.SCREENSHOT) {
            "Screenshot OCR extract only applies to SCREENSHOT assets."
        }
        require(engineId.isNotBlank())
        require(engineVersion.isNotBlank())
        require(integrity.isNotBlank())
        require(fullText.length <= MAX_STORED_CHARS) {
            "Stored OCR text must already be truncated to $MAX_STORED_CHARS characters."
        }
    }

    companion object {
        const val INTEGRITY_VERIFIED = "VERIFIED"
        const val MAX_STORED_CHARS = 50_000
        const val ENGINE_MLKIT_LATIN_BUNDLED = "mlkit-text-recognition-bundled"
    }
}

/** Outcome of opening a permitted screenshot for OCR. */
sealed interface ScreenshotOcrReadResult {
    data class Text(
        val fullText: String,
        val textTruncated: Boolean,
        val engineId: String,
        val engineVersion: String,
    ) : ScreenshotOcrReadResult

    data object AccessStopped : ScreenshotOcrReadResult

    data object RetryableFailure : ScreenshotOcrReadResult
}
