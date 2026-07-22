package com.memora.app.domain.extraction

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.SourceAccessState

/**
 * Pure authorization decision that must precede a future ordinary-process attempt
 * to open one approved PDF descriptor.
 *
 * It deliberately contains no URI, Android descriptor, stream, parser, or platform
 * API. The platform adapter must freshly obtain [accessState] immediately before
 * calling this contract and immediately before it opens the source. A successful
 * decision carries only immutable identity/version facts, never a source location.
 */
object ApprovedPdfDescriptorCustodyContract {
    fun decide(
        request: PdfExtractionRequest,
        approval: DocumentTreeApproval,
        accessState: SourceAccessState,
    ): PdfDescriptorCustodyDecision {
        if (request.asset.identity.sourceId != approval.sourceId) {
            return PdfDescriptorCustodyDecision.SourceMismatch
        }

        return when (accessState) {
            SourceAccessState.GRANTED -> PdfDescriptorCustodyDecision.Authorized(
                assetIdentity = request.asset.identity,
                assetFingerprint = request.asset.fingerprint,
            )

            SourceAccessState.ACCESS_REQUIRED -> PdfDescriptorCustodyDecision.AccessRequired
            SourceAccessState.ACCESS_REVOKED -> PdfDescriptorCustodyDecision.AccessRevoked
            SourceAccessState.UNAVAILABLE -> PdfDescriptorCustodyDecision.SourceUnavailable
        }
    }
}

/**
 * Content-free result for the future ordinary-process descriptor broker.
 *
 * [Authorized] proves only that the request is associated with the approved source
 * and the freshly observed access state. The broker still owns the later platform
 * checks that the document location belongs to that tree, the read-only open, and
 * descriptor closure. It must not pass a URI or this domain result to the isolated
 * parser service.
 */
sealed interface PdfDescriptorCustodyDecision {
    data class Authorized(
        val assetIdentity: AssetIdentity,
        val assetFingerprint: AssetFingerprint,
    ) : PdfDescriptorCustodyDecision

    data object AccessRequired : PdfDescriptorCustodyDecision

    data object AccessRevoked : PdfDescriptorCustodyDecision

    data object SourceUnavailable : PdfDescriptorCustodyDecision

    data object SourceMismatch : PdfDescriptorCustodyDecision
}
