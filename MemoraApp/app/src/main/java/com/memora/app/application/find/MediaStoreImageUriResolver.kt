package com.memora.app.application.find

import android.net.Uri

/**
 * Ordered reopen candidates for a persisted MediaStore image.
 *
 * IDs and volumes can shift after a cold boot while Asset rows stay put.
 * Implementations must not write or scan the library.
 *
 * Application orchestration uses [candidateLocations] so JVM tests do not
 * depend on Android [Uri.parse] stubs. The Android adapter owns parsing.
 */
interface MediaStoreImageUriResolver {
    fun candidates(storedUri: Uri, displayName: String): List<Uri>

    fun candidateLocations(storedLocation: String, displayName: String): List<String> {
        if (storedLocation.isBlank() || displayName.isBlank()) return emptyList()
        val parsed = runCatching { Uri.parse(storedLocation) }.getOrNull() ?: return emptyList()
        if (parsed.scheme.isNullOrBlank()) return emptyList()
        return candidates(storedUri = parsed, displayName = displayName).map { it.toString() }
    }
}
