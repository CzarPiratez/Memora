package com.memora.app.data.notes

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.memora.app.application.notes.ExternalUrlLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens OneNote/browser URLs via ACTION_VIEW. Does not use Graph contentUrl.
 *
 * On a phone with OneNote installed, the `onenote:` client link is preferred so
 * the native app opens. Emulators without OneNote fall back to the web URL.
 */
@Singleton
class AndroidExternalUrlLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ExternalUrlLauncher {
    override fun launch(url: String): Boolean = startView(url)

    override fun launchOneNoteOriginal(webUrl: String?, clientUrl: String?): Boolean {
        val client = clientUrl?.trim()?.takeIf { it.isNotEmpty() }
        val web = webUrl?.trim()?.takeIf { it.isNotEmpty() }
        if (client != null && canResolve(client) && startView(client)) {
            return true
        }
        if (web != null && startView(web)) {
            return true
        }
        // Last resort: try the client link even if resolve was uncertain.
        if (client != null && startView(client)) {
            return true
        }
        return false
    }

    private fun canResolve(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        if (uri.scheme.isNullOrBlank()) return false
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ) != null
    }

    private fun startView(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return false
        if (uri.scheme.isNullOrBlank()) return false
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
