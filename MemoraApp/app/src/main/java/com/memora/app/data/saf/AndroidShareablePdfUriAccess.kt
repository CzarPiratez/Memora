package com.memora.app.data.saf

import com.memora.app.application.share.ReadableContentUri
import com.memora.app.application.share.ShareablePdfUri
import com.memora.app.application.share.ShareablePdfUriAccess
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
class AndroidShareablePdfUriAccess @Inject constructor(
    private val assetRepository: AssetRepository,
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val readable: ReadableContentUri,
) : ShareablePdfUriAccess {
    override suspend fun resolve(
        sourceId: String,
        sourceAssetKey: String,
    ): ShareablePdfUri {
        val asset = assetRepository.find(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        )?.asset ?: return ShareablePdfUri.SourceUnavailable
        if (asset.type != AssetType.PDF) return ShareablePdfUri.CouldNotShare
        val approval = approvalRepository.find(asset.identity.sourceId)
            ?: return ShareablePdfUri.SourceUnavailable
        val target = when (
            val result = SafPdfCanonicalDocumentTargetFactory.create(approval, asset)
        ) {
            is SafPdfCanonicalTargetResult.Target -> result.value
            SafPdfCanonicalTargetResult.SourceMismatch,
            SafPdfCanonicalTargetResult.ForeignAssetLocation,
            SafPdfCanonicalTargetResult.InvalidApprovedTree,
            SafPdfCanonicalTargetResult.NotPdf,
            -> return ShareablePdfUri.CouldNotShare
        }
        return if (readable.canRead(target.documentUri.toString())) {
            ShareablePdfUri.Ready(target.documentUri.toString())
        } else {
            ShareablePdfUri.SourceUnavailable
        }
    }
}
