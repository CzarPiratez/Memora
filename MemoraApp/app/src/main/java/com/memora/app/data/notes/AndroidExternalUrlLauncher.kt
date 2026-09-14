package com.memora.app.data.notes

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.memora.app.application.notes.ExternalUrlLauncher
import com.memora.app.application.notes.OneNoteOpenTargetPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens OneNote/browser URLs via ACTION_VIEW. Does not use Graph contentUrl.
 *
 * The `onenote:` client link is attempted first so the native app opens. It is
 * not queried first: Android 11+ package visibility hides an installed OneNote
 * from `resolveActivity` unless declared in `<queries>`, which sent every tap
 * to the browser. A deep link nothing can handle throws, and the web URL runs.
 */
@Singleton
class AndroidExternalUrlLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ExternalUrlLauncher {
    override fun launch(url: String): Boolean = startView(url)

    override fun launchOneNoteOriginal(webUrl: String?, clientUrl: String?): Boolean =
        OneNoteOpenTargetPolicy.orderedTargets(webUrl = webUrl, clientUrl = clientUrl)
            .any { startView(it) }

    private fun startView(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return false
        if (uri.scheme.isNullOrBlank()) return false
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (OneNoteOpenTargetPolicy.needsBrowsableCategory(uri.scheme)) {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
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
