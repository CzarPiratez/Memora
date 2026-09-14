package com.memora.app.application.documents

import kotlin.math.max
import kotlin.math.min

/**
 * Fit a PDF page into a square edge budget without upscaling past 2.5×.
 */
object PdfPreviewScale {
    const val OPEN_MAX_EDGE_PX = 1440

    fun scale(pageWidthPt: Int, pageHeightPt: Int, maxEdgePx: Int): Float {
        require(maxEdgePx > 0)
        val w = max(1, pageWidthPt).toFloat()
        val h = max(1, pageHeightPt).toFloat()
        val edge = maxEdgePx.toFloat()
        val widthScale = edge / w
        val heightScale = edge / h
        return min(widthScale, heightScale).coerceAtMost(2.5f)
    }
}
