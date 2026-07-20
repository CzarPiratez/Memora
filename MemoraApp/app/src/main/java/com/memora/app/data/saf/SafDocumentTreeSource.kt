package com.memora.app.data.saf

import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeApproval
import java.security.MessageDigest
import java.time.Instant

/**
 * Creates the stable, private source identity for one Android SAF document tree.
 *
 * This class deliberately does not call ContentResolver or open a document. The
 * caller must obtain the URI only from Android's explicit document-tree approval UI.
 */
object SafDocumentTreeSource {
    private const val SOURCE_ID_PREFIX = "android-saf-document-tree:"

    fun approvalFor(
        persistedTreeUri: String,
        approvedAt: Instant,
    ): DocumentTreeApproval = DocumentTreeApproval(
        sourceId = sourceIdFor(persistedTreeUri),
        treeUri = persistedTreeUri,
        approvedAt = approvedAt,
    )

    fun sourceIdFor(persistedTreeUri: String): SourceId {
        require(persistedTreeUri.isNotBlank()) {
            "An approved document tree URI cannot be blank."
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(persistedTreeUri.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

        return SourceId(SOURCE_ID_PREFIX + digest)
    }
}
