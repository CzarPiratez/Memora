package com.memora.app.application.preview

import com.memora.app.application.documents.PdfPagePreviewRenderResult
import com.memora.app.application.images.PhotoPreviewRenderResult
import com.memora.app.application.images.ScreenshotPreviewRenderResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReloadOriginalPreviewTest {
    @Test
    fun pdf_reload_forwards_edge_and_keeps_pixels() = runTest {
        var capturedEdge = 0
        val pixels = IntArray(4) { 0xFF00FF00.toInt() }
        val reload = ReloadOriginalPreview(
            openPdf = { _, _, page, _, maxEdgePx, _ ->
                capturedEdge = maxEdgePx
                PdfPagePreviewRenderResult.Ready(
                    documentLabel = "pool.pdf",
                    pageNumber = page,
                    pageCount = 3,
                    widthPx = 2,
                    heightPx = 2,
                    argb8888 = pixels,
                )
            },
            openPhoto = { _, _, _, _ -> PhotoPreviewRenderResult.CouldNotOpen },
            openScreenshot = { _, _, _, _ -> ScreenshotPreviewRenderResult.CouldNotOpen },
        )
        val result = reload(
            OriginalPreviewReloadRequest.Pdf("s", "k", 2, "pool.pdf"),
            maxEdgePx = 2048,
        )
        assertEquals(2048, capturedEdge)
        val ready = result as OriginalPreviewReloadResult.Ready
        assertEquals(2, ready.widthPx)
        assertTrue(ready.argb8888.contentEquals(pixels))
    }

    @Test
    fun out_of_memory_keeps_the_preview_instead_of_crashing() = runTest {
        val reload = ReloadOriginalPreview(
            openPdf = { _, _, _, _, _, _ -> throw OutOfMemoryError("2048 px ARGB") },
            openPhoto = { _, _, _, _ -> throw OutOfMemoryError("2048 px ARGB") },
            openScreenshot = { _, _, _, _ -> ScreenshotPreviewRenderResult.CouldNotOpen },
        )
        assertEquals(
            OriginalPreviewReloadResult.Unavailable,
            reload(
                OriginalPreviewReloadRequest.Pdf("s", "k", 1, "pool.pdf"),
                maxEdgePx = 2048,
            ),
        )
        assertEquals(
            OriginalPreviewReloadResult.Unavailable,
            reload(
                OriginalPreviewReloadRequest.Photo("s", "k", "receipt.jpg"),
                maxEdgePx = 2048,
            ),
        )
    }

    @Test
    fun failed_open_is_unavailable_not_an_exception() = runTest {
        val reload = ReloadOriginalPreview(
            openPdf = { _, _, _, _, _, _ -> PdfPagePreviewRenderResult.SourceUnavailable },
            openPhoto = { _, _, _, _ -> PhotoPreviewRenderResult.CouldNotOpen },
            openScreenshot = { _, _, _, _ -> ScreenshotPreviewRenderResult.SourceUnavailable },
        )
        assertEquals(
            OriginalPreviewReloadResult.Unavailable,
            reload(
                OriginalPreviewReloadRequest.Photo("s", "k", "receipt.jpg"),
                maxEdgePx = 1280,
            ),
        )
        assertEquals(
            OriginalPreviewReloadResult.Unavailable,
            reload(
                OriginalPreviewReloadRequest.Screenshot("s", "k", "shot.png"),
                maxEdgePx = 1280,
            ),
        )
    }
}
