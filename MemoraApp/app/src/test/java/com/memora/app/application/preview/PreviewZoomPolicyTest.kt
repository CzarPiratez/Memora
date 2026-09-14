package com.memora.app.application.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewZoomPolicyTest {
    @Test
    fun scale_one_keeps_the_open_buffer() {
        assertEquals(
            PreviewZoomPolicy.IMAGE_INITIAL_EDGE_PX,
            PreviewZoomPolicy.renderEdgePx(1f, PreviewZoomPolicy.IMAGE_INITIAL_EDGE_PX),
        )
        assertEquals(
            PreviewZoomPolicy.PDF_INITIAL_EDGE_PX,
            PreviewZoomPolicy.renderEdgePx(1.1f, PreviewZoomPolicy.PDF_INITIAL_EDGE_PX),
        )
        assertFalse(
            PreviewZoomPolicy.needsReload(
                PreviewZoomPolicy.IMAGE_INITIAL_EDGE_PX,
                PreviewZoomPolicy.IMAGE_INITIAL_EDGE_PX,
            ),
        )
    }

    @Test
    fun pinch_past_reload_min_steps_up_in_buckets_and_caps() {
        assertEquals(
            1280,
            PreviewZoomPolicy.renderEdgePx(1.2f, PreviewZoomPolicy.IMAGE_INITIAL_EDGE_PX),
        )
        assertEquals(
            PreviewZoomPolicy.MAX_RENDER_EDGE_PX,
            PreviewZoomPolicy.renderEdgePx(4f, PreviewZoomPolicy.IMAGE_INITIAL_EDGE_PX),
        )
        assertEquals(
            PreviewZoomPolicy.MAX_RENDER_EDGE_PX,
            PreviewZoomPolicy.renderEdgePx(2f, PreviewZoomPolicy.PDF_INITIAL_EDGE_PX),
        )
        assertTrue(
            PreviewZoomPolicy.needsReload(
                PreviewZoomPolicy.MAX_RENDER_EDGE_PX,
                PreviewZoomPolicy.PDF_INITIAL_EDGE_PX,
            ),
        )
    }

    @Test
    fun buttons_and_double_tap_stay_inside_range() {
        assertEquals(1f, PreviewZoomPolicy.clampScale(0.4f), 0.0001f)
        assertEquals(4f, PreviewZoomPolicy.clampScale(9f), 0.0001f)
        assertEquals(1.5f, PreviewZoomPolicy.zoomIn(1f), 0.0001f)
        assertEquals(1f, PreviewZoomPolicy.zoomOut(1f), 0.0001f)
        assertEquals(4f, PreviewZoomPolicy.zoomIn(3.8f), 0.0001f)
        assertEquals(2.5f, PreviewZoomPolicy.toggleDoubleTap(1f), 0.0001f)
        assertEquals(1f, PreviewZoomPolicy.toggleDoubleTap(2.5f), 0.0001f)
        assertTrue(PreviewZoomPolicy.canZoomIn(1f))
        assertFalse(PreviewZoomPolicy.canZoomOut(1f))
        assertFalse(PreviewZoomPolicy.canZoomIn(4f))
        assertTrue(PreviewZoomPolicy.canZoomOut(4f))
    }

    @Test
    fun pan_is_zero_when_fitted_image_fills_the_viewport() {
        val (x, y) = PreviewZoomPolicy.constrainOffset(
            offsetX = 40f,
            offsetY = -30f,
            scale = 1f,
            viewportWidthPx = 400f,
            viewportHeightPx = 800f,
            fittedHeightPx = 500f,
        )
        assertEquals(0f, x, 0.01f)
        assertEquals(0f, y, 0.01f)
    }

    @Test
    fun tall_screenshot_can_pan_vertically_at_fit() {
        val (x, y) = PreviewZoomPolicy.constrainOffset(
            offsetX = 20f,
            offsetY = -200f,
            scale = 1f,
            viewportWidthPx = 400f,
            viewportHeightPx = 400f,
            fittedHeightPx = 1000f,
        )
        assertEquals(0f, x, 0.01f)
        assertEquals(-200f, y, 0.01f)
    }
}
