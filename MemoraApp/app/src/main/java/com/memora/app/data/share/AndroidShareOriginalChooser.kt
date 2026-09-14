package com.memora.app.data.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.memora.app.application.share.ShareOriginalChooser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only ACTION_SEND chooser. Does not copy or write the original.
 */
@Singleton
class AndroidShareOriginalChooser @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ShareOriginalChooser {
    override fun present(uri: String, mimeType: String, label: String): Boolean {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return false
        if (parsed.scheme.isNullOrBlank()) return false
        if (mimeType.isBlank() || label.isBlank()) return false
        val share = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, parsed)
            putExtra(Intent.EXTRA_SUBJECT, label)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(label, parsed)
        }
        val chooser = Intent.createChooser(share, label).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }
}
