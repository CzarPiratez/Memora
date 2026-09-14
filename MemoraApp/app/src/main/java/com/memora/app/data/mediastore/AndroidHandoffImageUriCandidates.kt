package com.memora.app.data.mediastore

import android.net.Uri
import com.memora.app.application.find.MediaStoreImageUriResolver
import com.memora.app.application.handoff.HandoffImageUriCandidates
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidHandoffImageUriCandidates @Inject constructor(
    private val resolver: MediaStoreImageUriResolver,
) : HandoffImageUriCandidates {
    override fun candidates(storedUri: String, displayName: String): List<String> {
        val parsed = runCatching { Uri.parse(storedUri) }.getOrNull() ?: return emptyList()
        if (parsed.scheme.isNullOrBlank()) return emptyList()
        return resolver.candidates(storedUri = parsed, displayName = displayName).map { it.toString() }
    }
}
