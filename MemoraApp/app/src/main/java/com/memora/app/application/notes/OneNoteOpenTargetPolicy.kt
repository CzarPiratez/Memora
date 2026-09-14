package com.memora.app.application.notes

/**
 * Order in which a saved note's Graph links are attempted.
 *
 * The OneNote app deep link goes first and is *attempted*, not queried.
 * Android 11+ package-visibility filtering hides an installed OneNote from
 * `resolveActivity` unless the app declares it in `<queries>`, so a query-first
 * launcher sent every person to the browser even with OneNote on the phone.
 * A launch that nothing can handle fails cheaply; the web URL is the fallback.
 */
object OneNoteOpenTargetPolicy {
    fun orderedTargets(webUrl: String?, clientUrl: String?): List<String> =
        listOfNotNull(
            clientUrl?.trim()?.takeIf { it.isNotEmpty() },
            webUrl?.trim()?.takeIf { it.isNotEmpty() },
        ).distinct()

    /**
     * `BROWSABLE` belongs to links a browser hands out. Requiring it on an
     * app scheme such as `onenote:` can exclude the very activity that handles
     * the deep link.
     */
    fun needsBrowsableCategory(scheme: String?): Boolean =
        when (scheme?.lowercase()) {
            "http", "https" -> true
            else -> false
        }
}
