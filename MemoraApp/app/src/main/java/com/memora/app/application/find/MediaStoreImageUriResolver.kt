package com.memora.app.application.find

import android.net.Uri

/**
 * Ordered reopen candidates for a persisted MediaStore image.
 *
 * IDs and volumes can shift after a cold boot while Asset rows stay put.
 * Implementations must not write or scan the library.
 */
interface MediaStoreImageUriResolver {
    fun candidates(storedUri: Uri, displayName: String): List<Uri>
}
