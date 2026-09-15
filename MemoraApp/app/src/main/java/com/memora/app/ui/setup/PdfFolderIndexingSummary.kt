package com.memora.app.ui.setup

/** Truthful copy for SAF PDF-folder metadata discovery (not text extraction). */
internal fun idlePdfFolderIndexingSummary(totalAssetCount: Int): String {
    val totalDescription = if (totalAssetCount == 1) "item" else "items"
    return "$totalAssetCount PDF $totalDescription already listed in this connected folder. " +
        "Tap Check for new PDFs after you add files. Text search still needs Local PDF reading " +
        "for each document."
}

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

internal const val PDF_FOLDER_INDEX_STOP_LABEL = "Stop"

/**
 * Android is holding the scan rather than running it. Saying "working" here is
 * what let a scan wait, or fail and back off, behind the same spinner.
 */
internal fun waitingPdfFolderIndexingSummary(retrying: Boolean): String = if (retrying) {
    "That pass did not finish, so UNFYND is waiting before trying again. " +
        "Nothing already listed was lost. You can stop and try later."
} else {
    "Android has scheduled this scan but is not running it yet — usually battery " +
        "saver or a busy phone. It will start on its own, or you can stop and try later."
}

internal const val PDF_FOLDER_INDEXING_STOPPED_BODY =
    "Folder scan stopped. Everything listed so far is kept, and UNFYND will pick up " +
        "where it left off when you check again."
