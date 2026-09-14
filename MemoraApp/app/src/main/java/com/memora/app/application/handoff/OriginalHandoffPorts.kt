package com.memora.app.application.handoff

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
fun interface HandoffImageUriCandidates {
    fun candidates(storedUri: String, displayName: String): List<String>
}

/**
 * Resolves the SAF document URI Open would use, for a read-only grant.
 *
 * Construction stays in the data adapter. Application never treats Asset.location
 * as the Open/handoff authority for PDFs.
 */
interface HandoffPdfUriAccess {
    suspend fun resolve(sourceId: String, sourceAssetKey: String): HandoffPdfUri
}

sealed interface HandoffPdfUri {
    data class Ready(val uri: String) : HandoffPdfUri {
        init {
            require(uri.isNotBlank()) { "A PDF handoff URI cannot be blank." }
        }
    }

    data object SourceUnavailable : HandoffPdfUri

    data object CouldNotHandOff : HandoffPdfUri
}

/**
 * Presents the Android share sheet. Read grant only; never a write grant.
 */
interface ShareOriginalChooser {
    fun present(uri: String, mimeType: String, label: String): Boolean
}

/**
 * Opens the whole original in whichever app the person already reads that file
 * type with. Read grant only; the viewer must not be handed a write grant.
 */
interface ExternalOriginalViewer {
    fun view(uri: String, mimeType: String, label: String): ExternalViewOutcome
}

sealed interface ExternalViewOutcome {
    data object Opened : ExternalViewOutcome

    /** Nothing on the phone handles this type. */
    data object NoAppAvailable : ExternalViewOutcome

    data object CouldNotOpen : ExternalViewOutcome
}
