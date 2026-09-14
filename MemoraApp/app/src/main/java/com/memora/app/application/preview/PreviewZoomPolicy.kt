package com.memora.app.application.preview

import com.memora.app.application.documents.PdfPreviewScale
import com.memora.app.application.images.OpenPersistedPhotoForViewing
import kotlin.math.roundToInt

/**
 * Pinch-zoom decode budget for an already-opened original.
 *
 * Scale is visual (pinch / buttons). Decode edge is a stepped pixel budget so
 * a continuous pinch does not re-open the file every frame. Cap is 2048 px
 * (~16 MB ARGB) to avoid OOM on a phone-class heap.
 */
object PreviewZoomPolicy {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 4f
    const val BUTTON_STEP = 0.5f
    const val DOUBLE_TAP_SCALE = 2.5f
    const val RELOAD_MIN_SCALE = 1.2f
    const val MAX_RENDER_EDGE_PX = 2048
    const val EDGE_BUCKET_PX = 256
    const val RERENDER_DEBOUNCE_MS = 120L

    const val PDF_INITIAL_EDGE_PX = PdfPreviewScale.OPEN_MAX_EDGE_PX
    const val IMAGE_INITIAL_EDGE_PX = OpenPersistedPhotoForViewing.MAX_PREVIEW_EDGE_PX

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    fun zoomIn(scale: Float): Float = clampScale(scale + BUTTON_STEP)

    fun zoomOut(scale: Float): Float = clampScale(scale - BUTTON_STEP)

    fun toggleDoubleTap(scale: Float): Float =
        if (scale < DOUBLE_TAP_SCALE - 0.01f) DOUBLE_TAP_SCALE else MIN_SCALE

    fun canZoomIn(scale: Float): Boolean = scale < MAX_SCALE - 0.001f

    fun canZoomOut(scale: Float): Boolean = scale > MIN_SCALE + 0.001f

    /**
     * Decode budget for [scale]. Scale 1 (and small pinches below
     * [RELOAD_MIN_SCALE]) keep the Open first-paint buffer already in memory.
     */
    fun renderEdgePx(scale: Float, initialEdgePx: Int): Int {
        require(initialEdgePx > 0) { "Initial Open edge must be positive." }
        val clamped = clampScale(scale)
        if (clamped < RELOAD_MIN_SCALE) return initialEdgePx
        val target = (initialEdgePx.toFloat() * clamped).roundToInt()
            .coerceAtMost(MAX_RENDER_EDGE_PX)
        if (target <= initialEdgePx) return initialEdgePx
        val bucket = ((target + EDGE_BUCKET_PX - 1) / EDGE_BUCKET_PX) * EDGE_BUCKET_PX
        return bucket.coerceIn(initialEdgePx, MAX_RENDER_EDGE_PX)
    }

    fun needsReload(renderEdgePx: Int, initialEdgePx: Int): Boolean {
        require(initialEdgePx > 0)
        return renderEdgePx > initialEdgePx
    }

    /**
     * Clamp pan so a centered, width-fitted image cannot be dragged into
     * empty space. Tall screenshots can pan vertically at scale 1.
     */
    fun constrainOffset(
        offsetX: Float,
        offsetY: Float,
        scale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        fittedHeightPx: Float,
    ): Pair<Float, Float> {
        if (viewportWidthPx <= 0f || viewportHeightPx <= 0f) return 0f to 0f
        val scaledWidth = viewportWidthPx * clampScale(scale)
        val scaledHeight = fittedHeightPx.coerceAtLeast(0f) * clampScale(scale)
        val extraX = ((scaledWidth - viewportWidthPx) / 2f).coerceAtLeast(0f)
        val extraY = ((scaledHeight - viewportHeightPx) / 2f).coerceAtLeast(0f)
        return offsetX.coerceIn(-extraX, extraX) to offsetY.coerceIn(-extraY, extraY)
    }
}
