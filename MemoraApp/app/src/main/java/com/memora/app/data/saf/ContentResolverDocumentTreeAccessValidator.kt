package com.memora.app.data.saf

import android.content.ContentResolver
import android.content.Context
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.SourceAccessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads Android's retained SAF grant list for one approved document tree.
 *
 * It intentionally does not contact a DocumentsProvider, enumerate a folder, or
 * open a document. A source operation must ask this boundary again immediately
 * before it uses the approved tree.
 */
class ContentResolverDocumentTreeAccessValidator(
    context: Context,
) : DocumentTreeAccessValidator {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    override suspend fun accessState(approval: DocumentTreeApproval): SourceAccessState =
        withContext(Dispatchers.IO) {
            val grants = resolver.persistedUriPermissions.map { permission ->
                PersistedDocumentTreeGrant(
                    treeUri = permission.uri.toString(),
                    hasReadPermission = permission.isReadPermission,
                )
            }
            if (PersistedDocumentTreeGrantMatcher.hasExactReadGrant(approval.treeUri, grants)) {
                SourceAccessState.GRANTED
            } else {
                SourceAccessState.ACCESS_REVOKED
            }
        }
}

/** A content-free snapshot of the Android permission properties relevant to Memora. */
internal data class PersistedDocumentTreeGrant(
    val treeUri: String,
    val hasReadPermission: Boolean,
)

/**
 * Pure exact-match rule shared by the Android adapter and unit tests.
 *
 * The stored approved URI must itself retain a read grant. A different tree or a
 * write-only grant never authorizes discovery or future descriptor opening.
 */
internal object PersistedDocumentTreeGrantMatcher {
    fun hasExactReadGrant(
        approvedTreeUri: String,
        grants: Iterable<PersistedDocumentTreeGrant>,
    ): Boolean = grants.any { grant ->
        grant.treeUri == approvedTreeUri && grant.hasReadPermission
    }
}
