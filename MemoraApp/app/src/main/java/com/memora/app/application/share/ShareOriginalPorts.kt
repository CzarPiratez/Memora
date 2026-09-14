package com.memora.app.application.share

/**
 * Read-only probe of a content URI. Implementations must not write or copy.
 */
interface ReadableContentUri {
    fun canRead(uri: String): Boolean

    fun mimeType(uri: String, fallback: String): String
}

/**
 * MediaStore reopen candidates as URI strings so application tests need no Android Uri.
 */
fun interface ShareImageUriCandidates {
    fun candidates(storedUri: String, displayName: String): List<String>
}

/**
 * Resolves the SAF document URI Open would use, for a read-only share grant.
 *
 * Construction stays in the data adapter. Application never treats Asset.location
 * as the Open/share authority for PDFs.
 */
interface ShareablePdfUriAccess {
    suspend fun resolve(sourceId: String, sourceAssetKey: String): ShareablePdfUri
}

sealed interface ShareablePdfUri {
    data class Ready(val uri: String) : ShareablePdfUri {
        init {
            require(uri.isNotBlank()) { "Shareable PDF URI cannot be blank." }
        }
    }

    data object SourceUnavailable : ShareablePdfUri

    data object CouldNotShare : ShareablePdfUri
}

/**
 * Presents the Android share sheet. Read grant only; never a write grant.
 */
interface ShareOriginalChooser {
    fun present(uri: String, mimeType: String, label: String): Boolean
}
