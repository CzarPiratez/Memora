package com.memora.app.data.saf

import com.memora.app.application.handoff.HandoffPdfUri
import com.memora.app.application.handoff.HandoffPdfUriAccess
import com.memora.app.application.handoff.ReadableContentUri
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands the same tree-document URI the Open broker would read, never Asset.location.
 */
@Singleton
class AndroidHandoffPdfUriAccess @Inject constructor(
    private val assetRepository: AssetRepository,
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val readable: ReadableContentUri,
) : HandoffPdfUriAccess {
    override suspend fun resolve(
        sourceId: String,
        sourceAssetKey: String,
    ): HandoffPdfUri {
        val asset = assetRepository.find(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        )?.asset ?: return HandoffPdfUri.SourceUnavailable
        if (asset.type != AssetType.PDF) return HandoffPdfUri.CouldNotHandOff
        val approval = approvalRepository.find(asset.identity.sourceId)
            ?: return HandoffPdfUri.SourceUnavailable
        val target = when (
            val result = SafPdfCanonicalDocumentTargetFactory.create(approval, asset)
        ) {
            is SafPdfCanonicalTargetResult.Target -> result.value
            SafPdfCanonicalTargetResult.SourceMismatch,
            SafPdfCanonicalTargetResult.ForeignAssetLocation,
            SafPdfCanonicalTargetResult.InvalidApprovedTree,
            SafPdfCanonicalTargetResult.NotPdf,
            -> return HandoffPdfUri.CouldNotHandOff
        }
        return if (readable.canRead(target.documentUri.toString())) {
            HandoffPdfUri.Ready(target.documentUri.toString())
        } else {
            HandoffPdfUri.SourceUnavailable
        }
    }
}
