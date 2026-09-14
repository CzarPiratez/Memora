package com.memora.app.application.find

import android.net.Uri

/**
 * Decodes a small ARGB thumbnail from a MediaStore or content URI.
 *
 * Implementations must not persist pixels. Callers pass already-resolved URI
 * candidates; this port does not invent locations.
 */
interface ImageThumbnailLoader {
    fun load(uri: Uri, maxEdgePx: Int): FindThumbnailResult.Ready?
}
