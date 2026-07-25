package com.memora.app.ui.setup

/** Truthful copy for SAF PDF-folder metadata discovery (not text extraction). */
internal fun completedPdfFolderIndexingSummary(
    discoveredAssetCount: Int,
    hasMore: Boolean,
): String {
    val itemDescription = if (discoveredAssetCount == 1) "item" else "items"
    val nextStep = if (hasMore) {
        "More folder metadata remains. Tap Continue folder indexing to keep listing documents. " +
            "This does not save PDF text for search yet."
    } else {
        "This connected folder's PDF list is currently up to date. " +
            "Text search still needs Local PDF reading for each document."
    }
    return "Memora indexed $discoveredAssetCount PDF $itemDescription from this connected folder. $nextStep"
}

internal const val PDF_FOLDER_INDEXING_IN_PROGRESS_BODY =
    "Memora is reading PDF folder metadata on this phone. " +
        "This lists documents only; it does not save PDF text for search yet."
