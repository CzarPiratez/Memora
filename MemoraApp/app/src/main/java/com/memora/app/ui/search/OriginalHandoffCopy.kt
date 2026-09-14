package com.memora.app.ui.search

/**
 * The two user-tapped handoffs on an opened original: Share and Open full file.
 *
 * One hint covers both because they are the same promise — another app gets a
 * read-only view of the file already on this phone. Not Act. Not an upload.
 */
object OriginalHandoffCopy {
    const val SHARE_LABEL = "Share"
    const val OPEN_LABEL = "Open full file"

    const val HINT_BODY =
        "Open full file and Share hand the original on this phone to an app you " +
            "choose — the whole file, not only this page. UNFYND hands over a " +
            "read-only view; it does not upload or change the file. Back brings " +
            "you here."

    const val SOURCE_UNAVAILABLE_BODY =
        "UNFYND cannot reach that file right now. Access may have changed."

    const val COULD_NOT_SHARE_BODY =
        "UNFYND could not share that file. The saved link may no longer be valid."

    const val COULD_NOT_OPEN_BODY =
        "UNFYND could not open that file. The saved link may no longer be valid."

    const val NO_APP_BODY =
        "No app on this phone opens this kind of file yet. The preview below " +
            "still works, and Share can pass it to an app that does."
}
