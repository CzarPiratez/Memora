package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType

@JvmInline
value class PhotoOcrSchemaVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "Photo OCR schema version cannot be blank." }
    }

    companion object {
        val V1 = PhotoOcrSchemaVersion("photo-ocr-v1")
    }
}

/** Durable deterministic OCR text for one PHOTO asset version. */
data class PhotoOcrExtractionRecord(
    val asset: Asset,
    val schemaVersion: PhotoOcrSchemaVersion,
    val fullText: String,
    val textTruncated: Boolean,
    val engineId: String,
    val engineVersion: String,
    val extractedAtEpochMillis: Long,
    val integrity: String = INTEGRITY_VERIFIED,
) {
    init {
        require(asset.type == AssetType.PHOTO) {
            "Photo OCR extract only applies to PHOTO assets."
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

sealed interface PhotoOcrReadResult {
    data class Text(
        val fullText: String,
        val textTruncated: Boolean,
        val engineId: String,
        val engineVersion: String,
    ) : PhotoOcrReadResult

    data object AccessStopped : PhotoOcrReadResult
    data object RetryableFailure : PhotoOcrReadResult
}
