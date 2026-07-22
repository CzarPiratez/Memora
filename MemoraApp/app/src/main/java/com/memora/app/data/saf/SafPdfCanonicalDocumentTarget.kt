package com.memora.app.data.saf

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.discovery.DocumentTreeApproval

/**
 * Derives the only URI a future SAF PDF descriptor broker may try to open.
 *
 * This is a platform-only canonicalization boundary. It performs no provider call,
 * permission check, descriptor open, source read, parser call, or persistence. The
 * future broker must still obtain a fresh grant and prove tree membership before it
 * uses [SafPdfCanonicalTarget.documentUri].
 */
internal object SafPdfCanonicalDocumentTargetFactory {
    fun create(
        approval: DocumentTreeApproval,
        asset: Asset,
    ): SafPdfCanonicalTargetResult {
        if (asset.type != AssetType.PDF) {
            return SafPdfCanonicalTargetResult.NotPdf
        }
        if (asset.identity.sourceId != approval.sourceId) {
            return SafPdfCanonicalTargetResult.SourceMismatch
        }

        val treeUri = Uri.parse(approval.treeUri)
        if (!treeUri.isValidContentTree()) {
            return SafPdfCanonicalTargetResult.InvalidApprovedTree
        }
        val discoveredLocation = Uri.parse(asset.location.value)
        if (
            discoveredLocation.scheme != ContentResolver.SCHEME_CONTENT ||
            discoveredLocation.authority != treeUri.authority
        ) {
            return SafPdfCanonicalTargetResult.ForeignAssetLocation
        }

        val treeDocumentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull()?.takeIf(String::isNotBlank)
            ?: return SafPdfCanonicalTargetResult.InvalidApprovedTree

        val canonicalDocumentUri = runCatching {
            DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                asset.identity.sourceAssetKey.value,
            )
        }.getOrNull()
            ?: return SafPdfCanonicalTargetResult.InvalidApprovedTree
        if (canonicalDocumentUri.authority != treeUri.authority) {
            return SafPdfCanonicalTargetResult.InvalidApprovedTree
        }

        return SafPdfCanonicalTargetResult.Target(
            SafPdfCanonicalTarget(
                treeUri = treeUri,
                treeDocumentId = treeDocumentId,
                documentUri = canonicalDocumentUri,
            ),
        )
    }

    private fun Uri.isValidContentTree(): Boolean =
        scheme == ContentResolver.SCHEME_CONTENT &&
            !authority.isNullOrBlank() &&
            DocumentsContract.isTreeUri(this)
}

/**
 * Android-only target facts retained inside the SAF data adapter.
 *
 * This is deliberately internal so a URI cannot flow into domain, application, UI,
 * Room, or the isolated parser service.
 */
internal data class SafPdfCanonicalTarget(
    val treeUri: Uri,
    val treeDocumentId: String,
    val documentUri: Uri,
)

/** Content-free denial outcomes for the future descriptor broker. */
internal sealed interface SafPdfCanonicalTargetResult {
    data class Target(val value: SafPdfCanonicalTarget) : SafPdfCanonicalTargetResult

    data object NotPdf : SafPdfCanonicalTargetResult

    data object SourceMismatch : SafPdfCanonicalTargetResult

    data object InvalidApprovedTree : SafPdfCanonicalTargetResult

    data object ForeignAssetLocation : SafPdfCanonicalTargetResult
}
