package com.memora.app.data.saf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.memora.app.application.documents.PdfPagePreviewRenderResult
import com.memora.app.application.documents.PdfPagePreviewRenderer
import javax.inject.Inject
import javax.inject.Singleton
import com.memora.app.application.documents.PdfPreviewScale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Read-only page preview via Android [PdfRenderer].
 *
 * Renders the cited 1-based page into ARGB8888 pixels for Compose display.
 * Never writes to the PDF.
 */
@Singleton
class AndroidPdfPagePreviewRenderer @Inject constructor() : PdfPagePreviewRenderer {
    override fun renderPage(
        descriptor: ParcelFileDescriptor,
        pageNumber: Int,
        documentLabel: String,
        maxEdgePx: Int,
    ): PdfPagePreviewRenderResult {
        if (pageNumber <= 0) return PdfPagePreviewRenderResult.CouldNotOpen

        return try {
            PdfRenderer(descriptor).use { renderer ->
                val pageCount = renderer.pageCount
                if (pageCount <= 0 || pageNumber > pageCount) {
                    return PdfPagePreviewRenderResult.CouldNotOpen
                }
                renderer.openPage(pageNumber - 1).use { page ->
                    val scale = PdfPreviewScale.scale(page.width, page.height, maxEdgePx)
                    val width = max(1, (page.width * scale).roundToInt())
                    val height = max(1, (page.height * scale).roundToInt())
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    bitmap.recycle()
                    PdfPagePreviewRenderResult.Ready(
                        documentLabel = documentLabel,
                        pageNumber = pageNumber,
                        pageCount = pageCount,
                        widthPx = width,
                        heightPx = height,
                        argb8888 = pixels,
                    )
                }
            }
        } catch (_: SecurityException) {
            PdfPagePreviewRenderResult.SourceUnavailable
        } catch (_: Exception) {
            PdfPagePreviewRenderResult.CouldNotOpen
        }
    }

}
