package com.memora.app.data.mediastore

import android.content.Context
import android.net.Uri
import com.memora.app.application.handoff.ReadableContentUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidReadableContentUri @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReadableContentUri {
    override fun canRead(uri: String): Boolean {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return false
        if (parsed.scheme.isNullOrBlank()) return false
        val resolver = context.applicationContext.contentResolver
        try {
            resolver.openFileDescriptor(parsed, "r")?.use { return true }
        } catch (_: SecurityException) {
            return false
        } catch (_: Exception) {
            // Fall through to stream open.
        }
        return try {
            resolver.openInputStream(parsed)?.use { true } == true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    override fun mimeType(uri: String, fallback: String): String {
        require(fallback.isNotBlank()) { "MIME fallback cannot be blank." }
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return fallback
        val resolved = try {
            context.applicationContext.contentResolver.getType(parsed)
        } catch (_: Exception) {
            null
        }
        return resolved?.takeIf { it.isNotBlank() } ?: fallback
    }
}
