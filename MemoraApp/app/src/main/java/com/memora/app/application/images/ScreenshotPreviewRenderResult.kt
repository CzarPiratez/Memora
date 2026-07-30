package com.memora.app.application.images

/**
 * Read-only in-app screenshot preview payload for a keyword hit.
 *
 * [Ready.argb8888] is JVM-testable pixel payload (not an Android Bitmap type).
 * Implementations must not write to the original file.
 */
sealed interface ScreenshotPreviewRenderResult {
    data class Ready(
        val screenshotLabel: String,
        val widthPx: Int,
        val heightPx: Int,
        val argb8888: IntArray,
    ) : ScreenshotPreviewRenderResult {
        init {
            require(screenshotLabel.isNotBlank()) { "Preview needs a screenshot label." }
            require(widthPx > 0 && heightPx > 0) { "Preview needs positive dimensions." }
            require(argb8888.size == widthPx * heightPx) {
                "Preview pixels must match width × height."
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Ready) return false
            return screenshotLabel == other.screenshotLabel &&
                widthPx == other.widthPx &&
                heightPx == other.heightPx &&
                argb8888.contentEquals(other.argb8888)
        }

        override fun hashCode(): Int {
            var result = screenshotLabel.hashCode()
            result = 31 * result + widthPx
            result = 31 * result + heightPx
            result = 31 * result + argb8888.contentHashCode()
            return result
        }
    }

    data object SourceUnavailable : ScreenshotPreviewRenderResult

    data object CouldNotOpen : ScreenshotPreviewRenderResult
}
