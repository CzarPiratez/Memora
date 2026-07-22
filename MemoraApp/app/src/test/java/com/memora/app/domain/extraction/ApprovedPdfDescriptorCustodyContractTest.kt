package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeSource
import com.memora.app.domain.discovery.SourceAccessState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ApprovedPdfDescriptorCustodyContractTest {
    private val approval = DocumentTreeSource.approvalFor(
        persistedTreeUri = "content://com.example.documents/tree/reports",
        approvedAt = Instant.parse("2026-07-23T00:00:00Z"),
    )
    private val request = PdfExtractionRequest(
        asset = asset(sourceId = approval.sourceId),
        schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
    )

    @Test
    fun `matching approved source with a fresh grant authorizes only identity and version facts`() {
        val result = ApprovedPdfDescriptorCustodyContract.decide(
            request = request,
            approval = approval,
            accessState = SourceAccessState.GRANTED,
        )

        assertEquals(
            PdfDescriptorCustodyDecision.Authorized(
                assetIdentity = request.asset.identity,
                assetFingerprint = request.asset.fingerprint,
            ),
            result,
        )
    }

    @Test
    fun `a request for a different source is rejected before any descriptor can be opened`() {
        val foreignRequest = PdfExtractionRequest(
            asset = asset(sourceId = SourceId("android-saf-document-tree:other")),
            schemaVersion = request.schemaVersion,
        )

        assertEquals(
            PdfDescriptorCustodyDecision.SourceMismatch,
            ApprovedPdfDescriptorCustodyContract.decide(
                request = foreignRequest,
                approval = approval,
                accessState = SourceAccessState.GRANTED,
            ),
        )
    }

    @Test
    fun `revoked access blocks a matching approved source`() {
        assertEquals(
            PdfDescriptorCustodyDecision.AccessRevoked,
            ApprovedPdfDescriptorCustodyContract.decide(
                request = request,
                approval = approval,
                accessState = SourceAccessState.ACCESS_REVOKED,
            ),
        )
    }

    @Test
    fun `access that must be requested is never treated as an open authorization`() {
        assertEquals(
            PdfDescriptorCustodyDecision.AccessRequired,
            ApprovedPdfDescriptorCustodyContract.decide(
                request = request,
                approval = approval,
                accessState = SourceAccessState.ACCESS_REQUIRED,
            ),
        )
    }

    @Test
    fun `unavailable source stays content free and retryable to a later adapter`() {
        assertEquals(
            PdfDescriptorCustodyDecision.SourceUnavailable,
            ApprovedPdfDescriptorCustodyContract.decide(
                request = request,
                approval = approval,
                accessState = SourceAccessState.UNAVAILABLE,
            ),
        )
    }

    private fun asset(sourceId: SourceId): Asset = Asset(
        identity = AssetIdentity(
            sourceId = sourceId,
            sourceAssetKey = SourceAssetKey("report-2026"),
        ),
        type = AssetType.PDF,
        location = AssetLocation("content://com.example.documents/document/report-2026"),
        fingerprint = AssetFingerprint("report-2026:42:1024:application/pdf"),
        discoveredAt = Instant.parse("2026-07-23T00:00:00Z"),
    )
}
