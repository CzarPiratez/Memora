package com.memora.app.ui.setup

/** Truthful completion copy for one explicit, bounded PDF-folder metadata page. */
internal fun completedPdfFolderIndexingSummary(
    discoveredAssetCount: Int,
    hasMore: Boolean,
): String {
    val itemDescription = if (discoveredAssetCount == 1) "item" else "items"
    val nextStep = if (hasMore) {
        "More folder metadata remains. You can choose another indexing step later."
    } else {
        "This connected folder is currently up to date."
    }
    return "Memora indexed $discoveredAssetCount PDF $itemDescription from this connected folder. $nextStep"
}
