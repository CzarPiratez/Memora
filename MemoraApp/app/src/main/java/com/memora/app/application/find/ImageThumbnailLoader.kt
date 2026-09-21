package com.memora.app.application.find

/**
 * One URI candidate for a Find-card image thumbnail.
 *
 * [Unreachable] is Open-class SourceUnavailable (file, descriptor, or grant
 * gone). [CouldNotDecode] is Open-class CouldNotOpen (opened or attempted,
 * but pixels did not come back). Callers persist availability only from
 * [Ready] and [Unreachable] after every candidate has been tried.
 */
sealed interface ImageThumbnailLoad {
    data class Ready(
        val widthPx: Int,
        val heightPx: Int,
        val argb8888: IntArray,
    ) : ImageThumbnailLoad {
        fun toFindResult(): FindThumbnailResult.Ready =
            FindThumbnailResult.Ready(
                widthPx = widthPx,
                heightPx = heightPx,
                argb8888 = argb8888,
            )
    }

    data object Unreachable : ImageThumbnailLoad

    data object CouldNotDecode : ImageThumbnailLoad
}

/**
 * Decodes a small ARGB thumbnail from a MediaStore or content location.
 *
 * Implementations must not persist pixels. Callers pass already-resolved
 * location strings; this port does not invent locations.
 */
interface ImageThumbnailLoader {
    fun load(location: String, maxEdgePx: Int): ImageThumbnailLoad
}
