package com.memora.app.data.saf

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeApproval
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises canonical target construction only. This test touches no provider,
 * permission, descriptor, source document, parser, database, UI, worker, AI, or
 * network; a later test-only DocumentsProvider fixture will cover opening.
 */
@RunWith(AndroidJUnit4::class)
class SafPdfCanonicalDocumentTargetFactoryIntegrationTest {
    private val approval = DocumentTreeApproval(
        sourceId = SourceId("android-saf-document-tree:approved"),
        treeUri = "content://com.memora.fixture.documents/tree/approved-root",
        approvedAt = Instant.parse("2026-07-23T00:00:00Z"),
    )

    @Test
    fun derives_the_open_target_from_the_approved_tree_and_opaque_source_key() {
        val result = SafPdfCanonicalDocumentTargetFactory.create(
            approval = approval,
            asset = asset(
                sourceAssetKey = "annual-report-2026",
                location = "content://com.memora.fixture.documents/document/untrusted-location",
            ),
        )

        val target = (result as SafPdfCanonicalTargetResult.Target).value

        assertEquals(approval.treeUri, target.treeUri.toString())
        assertEquals("approved-root", target.treeDocumentId)
        assertEquals(
            "content://com.memora.fixture.documents/tree/approved-root/document/annual-report-2026",
            target.documentUri.toString(),
        )
        assertFalse(target.documentUri.toString().contains("untrusted-location"))
    }

    @Test
    fun rejects_a_pdf_from_a_different_source_before_a_target_exists() {
        assertEquals(
            SafPdfCanonicalTargetResult.SourceMismatch,
            SafPdfCanonicalDocumentTargetFactory.create(
                approval = approval,
                asset = asset(sourceId = SourceId("android-saf-document-tree:other")),
            ),
        )
    }

    @Test
    fun rejects_a_non_pdf_asset_before_a_target_exists() {
        assertEquals(
            SafPdfCanonicalTargetResult.NotPdf,
            SafPdfCanonicalDocumentTargetFactory.create(
                approval = approval,
                asset = asset(type = AssetType.PHOTO),
            ),
        )
    }

    @Test
    fun rejects_a_non_content_or_non_tree_approval() {
        val invalidApproval = approval.copy(treeUri = "https://example.invalid/folder")

        assertEquals(
            SafPdfCanonicalTargetResult.InvalidApprovedTree,
            SafPdfCanonicalDocumentTargetFactory.create(
                approval = invalidApproval,
                asset = asset(sourceId = invalidApproval.sourceId),
            ),
        )
    }

    @Test
    fun rejects_a_location_from_a_different_provider_without_using_it_as_a_target() {
        assertEquals(
            SafPdfCanonicalTargetResult.ForeignAssetLocation,
            SafPdfCanonicalDocumentTargetFactory.create(
                approval = approval,
                asset = asset(location = "content://foreign.example/document/report"),
            ),
        )
    }

    private fun asset(
        sourceId: SourceId = approval.sourceId,
        sourceAssetKey: String = "report",
        type: AssetType = AssetType.PDF,
        location: String = "content://com.memora.fixture.documents/document/report",
    ): Asset = Asset(
        identity = AssetIdentity(
            sourceId = sourceId,
            sourceAssetKey = SourceAssetKey(sourceAssetKey),
        ),
        type = type,
        location = AssetLocation(location),
        fingerprint = AssetFingerprint("$sourceAssetKey:42:1024:application/pdf"),
        discoveredAt = Instant.parse("2026-07-23T00:00:00Z"),
    )
}
