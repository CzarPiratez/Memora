package com.memora.app.application.find

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.memory.CanonicalRecallResult
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAvailabilityStatus

/**
 * Bounds for Find-card thumbnails. Not a measured AVAILABLE SLA.
 *
 * Small enough that a ten-hit list cannot decode Open-preview 960px buffers.
 * Large enough to recognise a photo or a cited PDF page at list density.
 */
object FindThumbnailLimits {
    const val MAX_EDGE_PX = 128
    const val CACHE_ENTRIES = 24
    const val DECODE_PERMITS = 2
}

/**
 * Identity needed to reopen a stored original for a list thumbnail.
 *
 * [pageNumber] is the cited/matched PDF page when known. Photos, screenshots,
 * and notes leave it null. Missing PDF page renders page 1 and must be labelled
 * as a first-page preview, not a matched page.
 */
data class FindThumbnailRequest(
    val sourceId: String,
    val sourceAssetKey: String,
    val assetType: AssetType,
    val label: String,
    val pageNumber: Int?,
) {
    init {
        require(sourceId.isNotBlank())
        require(sourceAssetKey.isNotBlank())
        require(label.isNotBlank())
        require(pageNumber == null || pageNumber > 0)
    }

    val cacheKey: String
        get() = listOf(
            "v1",
            sourceId,
            sourceAssetKey,
            assetType.name,
            (pageNumber ?: 0).toString(),
            FindThumbnailLimits.MAX_EDGE_PX.toString(),
        ).joinToString("|")

    companion object {
        fun fromRecall(result: CanonicalRecallResult): FindThumbnailRequest =
            FindThumbnailRequest(
                sourceId = result.sourceId.value,
                sourceAssetKey = result.sourceAssetKey.value,
                assetType = result.assetType,
                label = result.label,
                pageNumber = result.openPageNumber,
            )

        fun fromMeaning(hit: MeaningSearchHit): FindThumbnailRequest =
            FindThumbnailRequest(
                sourceId = hit.sourceId.value,
                sourceAssetKey = hit.sourceAssetKey.value,
                assetType = hit.assetType,
                label = hit.label,
                pageNumber = hit.rankedPdfPageNumber ?: hit.citedPdfPageNumber,
            )
    }
}

enum class FindThumbnailGlyph {
    NOTE,
    /** Decode/permission miss — not a confirmed gone original. */
    UNAVAILABLE,
    /** Open-class reopen could not reach the original. */
    SOURCE_UNREACHABLE,
}

sealed interface FindThumbnailResult {
    data class Ready(
        val widthPx: Int,
        val heightPx: Int,
        val argb8888: IntArray,
        val renderedPageNumber: Int? = null,
        val pageWasCited: Boolean = false,
    ) : FindThumbnailResult {
        init {
            require(widthPx > 0 && heightPx > 0)
            require(argb8888.size == widthPx * heightPx) {
                "Thumbnail pixels must match width × height."
            }
            require(renderedPageNumber == null || renderedPageNumber > 0)
        }
    }

    data class Glyph(val kind: FindThumbnailGlyph) : FindThumbnailResult
}

/**
 * Last-known Open reachability learned from a list thumbnail reopen.
 * Null means the glyph is not a confirmed Open outcome (note, flake, permission).
 */
fun FindThumbnailResult.learnedAvailability(): SourceAvailabilityStatus? = when (this) {
    is FindThumbnailResult.Ready -> SourceAvailabilityStatus.REACHABLE
    is FindThumbnailResult.Glyph -> when (kind) {
        FindThumbnailGlyph.SOURCE_UNREACHABLE -> SourceAvailabilityStatus.UNREACHABLE
        FindThumbnailGlyph.NOTE,
        FindThumbnailGlyph.UNAVAILABLE,
        -> null
    }
}

/**
 * Pure rules for thumbnail caching and PDF page choice.
 *
 * Failures are not cached so a later permission grant can retry. Note glyphs
 * are deterministic and may be cached.
 */
object FindThumbnailPolicy {
    fun pdfPageToRender(requestedPageNumber: Int?): Int = requestedPageNumber ?: 1

    fun pageWasCited(requestedPageNumber: Int?): Boolean = requestedPageNumber != null

    fun cacheable(result: FindThumbnailResult): Boolean = when (result) {
        is FindThumbnailResult.Ready -> true
        is FindThumbnailResult.Glyph -> result.kind == FindThumbnailGlyph.NOTE
    }

    /**
     * After every image URI candidate missed: a decode flake must not mark
     * the original gone. Only a clean miss (all Unreachable) is Open-class.
     */
    fun imageMissGlyph(decodeFlake: Boolean): FindThumbnailGlyph =
        if (decodeFlake) FindThumbnailGlyph.UNAVAILABLE
        else FindThumbnailGlyph.SOURCE_UNREACHABLE
}
