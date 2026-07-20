package com.memora.app.ui.setup

import com.memora.app.domain.discovery.ImageLibraryAccessScope

/**
 * User-facing completion copy for one explicit, bounded MediaStore indexing request.
 *
 * This is pure presentation logic so the selected-photo privacy boundary remains
 * testable without requiring a device or accessing an Android source.
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
        "More permitted items remain. You can choose another indexing step later."
    } else {
        "This permitted source is currently up to date."
    }

    return "Memora indexed $discoveredAssetCount $itemDescription from $sourceDescription. $nextStep"
}
