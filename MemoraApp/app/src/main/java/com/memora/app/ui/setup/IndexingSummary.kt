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
        "More permitted items remain. Tap Start indexing to keep building your on-device photo catalogue."
    } else {
        "This permitted photo catalogue is up to date on this phone. " +
            "Next: read text from photos or screenshots, then build memories."
    }

    return "UNFYND indexed $discoveredAssetCount $itemDescription from $sourceDescription. $nextStep"
}

internal const val MEDIASTORE_INDEXING_IN_PROGRESS_BODY =
    "UNFYND is reading photo and screenshot metadata on this phone. " +
        "This builds your on-device photo catalogue."

internal const val MEDIASTORE_EXIF_EXTRACT_IN_PROGRESS_BODY =
    "UNFYND is reading basic photo facts (such as date and camera tags when present) " +
        "from permitted photos on this phone."

internal fun completedImageExifExtractSummary(
    extractedCount: Int,
    catalogueCount: Int,
): String {
    require(extractedCount >= 0 && catalogueCount >= 0)
    val items = if (extractedCount == 1) "photo" else "photos"
    return "UNFYND saved basic facts for $extractedCount $items " +
        "(from $catalogueCount catalogued items). " +
        "Date, camera, and size metadata stay on this phone."
}

internal const val MEDIASTORE_SCREENSHOT_OCR_IN_PROGRESS_BODY =
    "UNFYND is reading text from permitted screenshots on this phone. " +
        "OCR text stays on-device for exact-word search."

internal fun completedScreenshotOcrExtractSummary(
    extractedCount: Int,
    screenshotCatalogueCount: Int,
): String {
    require(extractedCount >= 0 && screenshotCatalogueCount >= 0)
    val items = if (extractedCount == 1) "screenshot" else "screenshots"
    return "UNFYND saved on-device text from $extractedCount $items " +
        "(from $screenshotCatalogueCount catalogued screenshots). " +
        "Search exact words from Find saved screenshot text on Welcome. " +
        "Ordinary photos use a separate text-reading step."
}

internal const val MEDIASTORE_PHOTO_OCR_IN_PROGRESS_BODY =
    "UNFYND is reading text from permitted ordinary photos on this phone. " +
        "OCR text stays on-device for exact-word search."

internal fun completedPhotoOcrExtractSummary(
    extractedCount: Int,
    photoCatalogueCount: Int,
): String {
    require(extractedCount >= 0 && photoCatalogueCount >= 0)
    val items = if (extractedCount == 1) "photo" else "photos"
    return "UNFYND saved on-device text from $extractedCount $items " +
        "(from $photoCatalogueCount catalogued photos). " +
        "Search exact words from Find saved photo text on Welcome."
}
