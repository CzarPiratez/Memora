package com.memora.app.ui.search

import com.memora.app.application.find.FindThumbnailGlyph
import com.memora.app.application.find.FindThumbnailRequest
import com.memora.app.application.find.FindThumbnailResult
import com.memora.app.domain.asset.AssetType

/**
 * Spoken and visible placeholder copy for Find-card thumbnails.
 *
 * Photo and screenshot pixels sit next to the filename, so TalkBack treats
 * them as decorative. A cited PDF page is extra information and is spoken.
 * Notes have no local page image on this phone.
 */
object FindThumbnailCopy {
    const val NOTE_GLYPH = "Note"

    const val UNAVAILABLE_GLYPH = "—"

    const val UNAVAILABLE_DESCRIPTION = "Preview unavailable"

    fun glyphLabel(kind: FindThumbnailGlyph): String = when (kind) {
        FindThumbnailGlyph.NOTE -> NOTE_GLYPH
        FindThumbnailGlyph.UNAVAILABLE -> UNAVAILABLE_GLYPH
    }

    fun spokenDescription(
        request: FindThumbnailRequest,
        result: FindThumbnailResult,
    ): String? = when (result) {
        is FindThumbnailResult.Glyph -> when (result.kind) {
            FindThumbnailGlyph.NOTE -> NOTE_GLYPH
            FindThumbnailGlyph.UNAVAILABLE -> UNAVAILABLE_DESCRIPTION
        }
        is FindThumbnailResult.Ready -> when (request.assetType) {
            AssetType.PDF -> {
                val page = result.renderedPageNumber ?: 1
                if (result.pageWasCited) {
                    "Page $page of ${request.label}"
                } else {
                    "First page of ${request.label}"
                }
            }
            AssetType.PHOTO, AssetType.SCREENSHOT, AssetType.NOTE -> null
        }
    }
}
