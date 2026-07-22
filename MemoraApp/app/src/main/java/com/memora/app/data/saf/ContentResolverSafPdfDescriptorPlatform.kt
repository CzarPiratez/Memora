package com.memora.app.data.saf

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract

/**
 * Android implementation of the descriptor broker's narrow platform operations.
 *
 * It has no parser, Room, source repository, or UI dependency. The broker validates
 * the exact source and fresh grant before calling this adapter. API 26-28 are denied
 * safely until their non-heuristic membership fallback has its own accepted test
 * fixture; no path-prefix substitute is permitted.
 */
internal class ContentResolverSafPdfDescriptorPlatform(
    context: Context,
) : SafPdfDescriptorPlatform {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    override fun membership(target: SafPdfCanonicalTarget): SafPdfTreeMembership {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return SafPdfTreeMembership.UNSUPPORTED
        }
        return try {
            val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
                target.treeUri,
                target.treeDocumentId,
            )
            if (DocumentsContract.isChildDocument(resolver, rootDocumentUri, target.documentUri)) {
                SafPdfTreeMembership.VERIFIED
            } else {
                SafPdfTreeMembership.REJECTED
            }
        } catch (_: SecurityException) {
            SafPdfTreeMembership.UNAVAILABLE
        } catch (_: IllegalArgumentException) {
            SafPdfTreeMembership.UNAVAILABLE
        } catch (_: RuntimeException) {
            SafPdfTreeMembership.UNAVAILABLE
        }
    }

    override fun openReadOnly(
        target: SafPdfCanonicalTarget,
        cancellationSignal: CancellationSignal?,
    ): ParcelFileDescriptor? = resolver.openFileDescriptor(
        target.documentUri,
        "r",
        cancellationSignal,
    )
}
