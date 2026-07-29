package com.memora.app.ui.setup

import com.memora.app.domain.discovery.ImageLibraryAccessScope

/**
 * User-facing completion copy for MediaStore photo/screenshot metadata discovery.
 *
 * Pure presentation logic so the selected-photo privacy boundary remains testable
 * without requiring a device. Never claims OCR, Memory, or searchable understanding.
 */
internal fun completedIndexingSummary(
    discoveredAssetCount: Int,
    hasMore: Boolean,
    accessScope: ImageLibraryAccessScope,
): String {
    val sourceDescription = when (accessScope) {
        ImageLibraryAccessScope.FULL_LIBRARY -> "your permitted photo library"
        ImageLibraryAccessScope.SELECTED_PHOTOS -> "only the photos you selected"
    }
    val itemDescription = if (discoveredAssetCount == 1) "item" else "items"
    val nextStep = if (hasMore) {
        "More permitted items remain. Tap Start indexing to keep listing photo metadata. " +
            "This does not read photo contents or create searchable memories yet."
    } else {
        "This permitted photo catalogue is currently up to date. " +
            "This lists metadata only; it does not read photo contents or create searchable memories yet."
    }

    return "Memora indexed $discoveredAssetCount $itemDescription from $sourceDescription. $nextStep"
}

internal const val MEDIASTORE_INDEXING_IN_PROGRESS_BODY =
    "Memora is reading photo and screenshot metadata on this phone. " +
        "This lists items only; it does not open photo contents or create searchable memories yet."

internal const val MEDIASTORE_EXIF_EXTRACT_IN_PROGRESS_BODY =
    "Memora is reading basic photo facts (such as date and camera tags when present) " +
        "from permitted photos on this phone. This does not read text from images, " +
        "run OCR, or create searchable memories yet."

internal fun completedImageExifExtractSummary(
    extractedCount: Int,
    catalogueCount: Int,
): String {
    require(extractedCount >= 0 && catalogueCount >= 0)
    val items = if (extractedCount == 1) "photo" else "photos"
    return "Memora saved basic facts for $extractedCount $items " +
        "(from $catalogueCount catalogued items). " +
        "This is EXIF and size metadata only — not OCR, keyword search, or meaning-based recall."
}

internal const val MEDIASTORE_SCREENSHOT_OCR_IN_PROGRESS_BODY =
    "Memora is reading text from permitted screenshots on this phone. " +
        "This stores OCR text on-device only. It does not open a keyword search " +
        "or create meaning-based memories yet."

internal fun completedScreenshotOcrExtractSummary(
    extractedCount: Int,
    screenshotCatalogueCount: Int,
): String {
    require(extractedCount >= 0 && screenshotCatalogueCount >= 0)
    val items = if (extractedCount == 1) "screenshot" else "screenshots"
    return "Memora saved on-device text from $extractedCount $items " +
        "(from $screenshotCatalogueCount catalogued screenshots). " +
        "This is OCR text only — not meaning-based recall. " +
        "You can search those words from Find saved screenshot text on the welcome screen. " +
        "Ordinary photos are not OCR’d in this step."
}
