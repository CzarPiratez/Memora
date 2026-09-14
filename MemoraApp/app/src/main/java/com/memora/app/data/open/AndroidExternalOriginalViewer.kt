package com.memora.app.data.open

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.memora.app.application.handoff.ExternalOriginalViewer
import com.memora.app.application.handoff.ExternalViewOutcome
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only `ACTION_VIEW` into whichever app the person already uses for the
 * type. No chooser is forced, so their default reader opens.
 *
 * Grants a read of the stored URI for this one intent. Never copies the file
 * into UNFYND storage, never asks for write, never mutates the original.
 */
@Singleton
class AndroidExternalOriginalViewer @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val foregroundActivity: ForegroundActivityTracker,
) : ExternalOriginalViewer {
    override fun view(uri: String, mimeType: String, label: String): ExternalViewOutcome {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull()
            ?: return ExternalViewOutcome.CouldNotOpen
        if (parsed.scheme.isNullOrBlank()) return ExternalViewOutcome.CouldNotOpen
        if (mimeType.isBlank() || label.isBlank()) return ExternalViewOutcome.CouldNotOpen
        val host = foregroundActivity.current()
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(parsed, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Some readers take the URI from the clip instead of the data field.
            clipData = ClipData(label, arrayOf(mimeType), ClipData.Item(parsed))
            if (host == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            (host ?: appContext).startActivity(view)
            ExternalViewOutcome.Opened
        } catch (_: ActivityNotFoundException) {
            ExternalViewOutcome.NoAppAvailable
        } catch (_: Exception) {
            ExternalViewOutcome.CouldNotOpen
        }
    }
}
