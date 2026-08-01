package com.memora.app.application.notes

/** Opens a user-facing URL outside Memora (browser or OneNote client). */
interface ExternalUrlLauncher {
    /**
     * @return true when an activity was started; false when nothing could handle the URL.
     */
    fun launch(url: String): Boolean

    /**
     * Opens a OneNote original. Prefers the OneNote app deep link when the device
     * can handle it; otherwise opens the browser/web URL.
     */
    fun launchOneNoteOriginal(webUrl: String?, clientUrl: String?): Boolean
}
