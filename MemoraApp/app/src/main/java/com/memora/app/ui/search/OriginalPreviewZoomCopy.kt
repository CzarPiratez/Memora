package com.memora.app.ui.search

/**
 * Pinch-zoom chrome for an already-opened original. Does not claim search
 * re-read the file; Open already did a read-only preview.
 */
object OriginalPreviewZoomCopy {
    const val ZOOM_IN_LABEL = "Zoom in"
    const val ZOOM_OUT_LABEL = "Zoom out"
    const val FIT_LABEL = "Fit to screen"
    const val HINT_BODY =
        "Pinch or tap Zoom in to read more clearly. UNFYND re-reads the original " +
            "on this phone for a sharper picture; it does not upload it."
    const val SHARPENING_BODY =
        "Reading a sharper view from the original on this phone…"
    const val COULD_NOT_SHARPEN_BODY =
        "UNFYND could not read a sharper view from the original on this phone. " +
            "The picture below has not changed."
}
