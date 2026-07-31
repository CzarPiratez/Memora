package com.memora.app.application.images

sealed interface PhotoPreviewRenderResult {
    data class Ready(
        val photoLabel: String,
        val widthPx: Int,
        val heightPx: Int,
        val argb8888: IntArray,
    ) : PhotoPreviewRenderResult {
        init {
            require(photoLabel.isNotBlank())
            require(widthPx > 0 && heightPx > 0)
            require(argb8888.size == widthPx * heightPx)
        }
    }

    data object SourceUnavailable : PhotoPreviewRenderResult
    data object CouldNotOpen : PhotoPreviewRenderResult
}
