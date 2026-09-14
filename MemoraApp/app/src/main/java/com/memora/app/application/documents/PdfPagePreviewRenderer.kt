package com.memora.app.application.documents

import android.os.ParcelFileDescriptor

/**
 * Renders one page of a user PDF read-only for in-app preview.
 *
 * Implementations must not write to the original file. [Ready.argb8888] is
 * JVM-testable pixel payload (not an Android Bitmap type).
 */
interface PdfPagePreviewRenderer {
    fun renderPage(
        descriptor: ParcelFileDescriptor,
        pageNumber: Int,
        documentLabel: String,
        maxEdgePx: Int = PdfPreviewScale.OPEN_MAX_EDGE_PX,
    ): PdfPagePreviewRenderResult
}

sealed interface PdfPagePreviewRenderResult {
    data class Ready(
        val documentLabel: String,
        val pageNumber: Int,
        val pageCount: Int,
        val widthPx: Int,
        val heightPx: Int,
        val argb8888: IntArray,
    ) : PdfPagePreviewRenderResult {
        init {
            require(documentLabel.isNotBlank()) { "Preview needs a document label." }
            require(pageNumber > 0) { "Preview page must be positive." }
            require(pageCount > 0) { "Preview needs a positive page count." }
            require(pageNumber <= pageCount) { "Preview page must be within page count." }
            require(widthPx > 0 && heightPx > 0) { "Preview needs positive dimensions." }
            require(argb8888.size == widthPx * heightPx) {
                "Preview pixels must match width × height."
            }
        }
    }

    data object SourceUnavailable : PdfPagePreviewRenderResult

    data object CouldNotOpen : PdfPagePreviewRenderResult
}
