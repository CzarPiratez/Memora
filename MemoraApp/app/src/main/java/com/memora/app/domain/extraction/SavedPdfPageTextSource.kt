package com.memora.app.domain.extraction

/**
 * Saved PDF page text already extracted on this phone (current fingerprint).
 *
 * Used for meaning-open cue-best page selection without reopening the file for
 * ranking.
 */
data class SavedPdfPageText(
    val pageNumber: Int,
    val text: String,
) {
    init {
        require(pageNumber > 0)
        require(text.isNotBlank())
    }
}

fun interface SavedPdfPageTextSource {
    suspend fun listCurrentVerifiedPages(
        sourceId: String,
        sourceAssetKey: String,
    ): List<SavedPdfPageText>
}
