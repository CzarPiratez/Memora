package com.memora.app.ui.setup

/** Truthful copy for SAF PDF-folder metadata discovery (not text extraction). */
internal fun completedPdfFolderIndexingSummary(
    totalAssetCount: Int,
    newlyDiscoveredAssetCount: Int,
    hasMore: Boolean,
): String {
    val totalDescription = if (totalAssetCount == 1) "item" else "items"
    val newLine = when {
        newlyDiscoveredAssetCount <= 0 ->
            "No new PDFs were listed this pass. $totalAssetCount PDF $totalDescription " +
                "in this connected folder."
        newlyDiscoveredAssetCount == 1 ->
            "Found 1 new PDF this pass. $totalAssetCount PDF $totalDescription " +
                "in this connected folder."
        else ->
            "Found $newlyDiscoveredAssetCount new PDFs this pass. $totalAssetCount PDF " +
                "$totalDescription in this connected folder."
    }
    val nextStep = if (hasMore) {
        "More folder metadata remains. Tap Continue folder indexing to keep listing documents. " +
            "This does not save PDF text for search yet."
    } else {
        "This folder list is up to date for now. Text search still needs Local PDF reading " +
            "for each document. Tap Check for new PDFs after you add files."
    }
    return "$newLine $nextStep"
}

internal const val PDF_FOLDER_INDEX_FIRST_INDEX_LABEL = "Index this folder"

internal const val PDF_FOLDER_INDEX_CHECK_FOR_NEW_LABEL = "Check for new PDFs"

internal const val PDF_FOLDER_INDEX_CONTINUE_LABEL = "Continue folder indexing"

internal const val PDF_FOLDER_INDEXING_IN_PROGRESS_BODY =
    "UNFYND is reading PDF folder metadata on this phone. " +
        "This lists documents only; it does not save PDF text for search yet."
