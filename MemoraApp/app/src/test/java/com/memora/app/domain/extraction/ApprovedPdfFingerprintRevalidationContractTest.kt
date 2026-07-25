package com.memora.app.domain.extraction

import com.memora.app.domain.asset.AssetFingerprint
import org.junit.Assert.assertEquals
import org.junit.Test

class ApprovedPdfFingerprintRevalidationContractTest {
    @Test
    fun accepts_matching_observed_fingerprint() {
        val fingerprint = AssetFingerprint("doc:1:10:application/pdf")

        assertEquals(
            PdfFingerprintRevalidationDecision.Current,
            ApprovedPdfFingerprintRevalidationContract.decide(
                requestFingerprint = fingerprint,
                observedFingerprint = fingerprint,
            ),
        )
    }

    @Test
    fun rejects_changed_observed_fingerprint_as_stale() {
        assertEquals(
            PdfFingerprintRevalidationDecision.Stale,
            ApprovedPdfFingerprintRevalidationContract.decide(
                requestFingerprint = AssetFingerprint("doc:1:10:application/pdf"),
                observedFingerprint = AssetFingerprint("doc:2:10:application/pdf"),
            ),
        )
    }

    @Test
    fun rejects_missing_observation_as_unavailable() {
        assertEquals(
            PdfFingerprintRevalidationDecision.Unavailable,
            ApprovedPdfFingerprintRevalidationContract.decide(
                requestFingerprint = AssetFingerprint("doc:1:10:application/pdf"),
                observedFingerprint = null,
            ),
        )
    }
}
