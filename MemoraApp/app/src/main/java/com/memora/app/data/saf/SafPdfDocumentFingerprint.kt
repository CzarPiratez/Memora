package com.memora.app.data.saf

import com.memora.app.domain.asset.AssetFingerprint

/**
 * Shared deterministic fingerprint for SAF PDF Assets.
 *
 * Discovery and pre-open revalidation must use the same formula so a current document
 * is recognized and a changed document is refused before any descriptor open.
 */
internal object SafPdfDocumentFingerprint {
    private const val UNKNOWN_VERSION = "unknown-version"
    private const val UNKNOWN_SIZE = "unknown-size"

    fun from(
        documentId: String,
        lastModifiedEpochMillis: Long?,
        sizeBytes: Long?,
        mimeType: String,
    ): AssetFingerprint = AssetFingerprint(
        "$documentId:${lastModifiedEpochMillis ?: UNKNOWN_VERSION}:${sizeBytes ?: UNKNOWN_SIZE}:$mimeType",
    )
}
