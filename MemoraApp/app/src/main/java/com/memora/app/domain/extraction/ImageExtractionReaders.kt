package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset

/**
 * Opens a permitted PHOTO asset read-only and returns OCR text for extraction.
 *
 * Discovery must not call this (ADR-009). Extract path only. Implementations live
 * in platform adapters.
 */
fun interface PhotoOcrReader {
    val engineVersion: String get() = "16.0.1"
    fun read(asset: Asset): PhotoOcrReadResult
}

/**
 * Opens a permitted SCREENSHOT asset read-only and returns OCR text for extraction.
 *
 * Discovery must not call this (ADR-009). Extract path only.
 */
fun interface ScreenshotOcrReader {
    fun read(asset: Asset): ScreenshotOcrReadResult
}

/**
 * Opens a permitted image asset read-only and reads EXIF facts (not OCR).
 *
 * Discovery must not call this (ADR-009). Extract path only.
 */
fun interface ImageExifReader {
    fun read(asset: Asset): ImageExifReadResult
}
