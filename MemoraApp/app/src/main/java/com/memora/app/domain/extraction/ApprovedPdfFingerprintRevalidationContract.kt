package com.memora.app.domain.extraction

import com.memora.app.domain.asset.AssetFingerprint

/**
 * Pure decision that the Asset fingerprint still matches freshly observed source metadata.
 *
 * It has no URI, descriptor, stream, parser, or Android dependency. The broker must
 * call this immediately before opening a descriptor so a changed document is refused
 * instead of parsed as an older Asset version.
 */
object ApprovedPdfFingerprintRevalidationContract {
    fun decide(
        requestFingerprint: AssetFingerprint,
        observedFingerprint: AssetFingerprint?,
    ): PdfFingerprintRevalidationDecision {
        if (observedFingerprint == null) {
            return PdfFingerprintRevalidationDecision.Unavailable
        }
        return if (requestFingerprint == observedFingerprint) {
            PdfFingerprintRevalidationDecision.Current
        } else {
            PdfFingerprintRevalidationDecision.Stale
        }
    }
}

/** Content-free fingerprint check outcome for the ordinary-process broker. */
sealed interface PdfFingerprintRevalidationDecision {
    data object Current : PdfFingerprintRevalidationDecision

    data object Stale : PdfFingerprintRevalidationDecision

    data object Unavailable : PdfFingerprintRevalidationDecision
}
