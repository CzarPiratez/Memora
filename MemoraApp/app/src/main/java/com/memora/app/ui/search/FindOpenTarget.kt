package com.memora.app.ui.search

/**
 * Which result card an Open-original attempt belongs to.
 *
 * Open feedback used to be one screen-level flag. Every card read the same
 * "an open is in progress" boolean, so tapping Open on the third result
 * greyed out every Open button at once — the list looked like it had all been
 * tapped — while the only progress indicator rendered above the search box,
 * off-screen for anyone who had scrolled far enough to reach a result. A note
 * open waits on Microsoft Graph, so that silent window lasts seconds.
 *
 * Feedback therefore travels with the identity of the card it came from, and a
 * card renders progress and failure inside itself. The identity is the source
 * identity because that is what the result list is deduplicated by
 * (`SearchAssetMemoriesByMeaning` groups on `sourceId|sourceAssetKey`), so two
 * cards on one screen can never share one.
 */
data class FindOpenTarget(
    val sourceId: String,
    val sourceAssetKey: String,
) {
    init {
        require(sourceId.isNotBlank()) { "An open target needs the source it came from." }
        require(sourceAssetKey.isNotBlank()) { "An open target needs the asset it points at." }
    }
}

/**
 * What one result card should show about its own Open attempt. Screens map the
 * failure cases onto their own copy — a note that needs its OneNote connection
 * renewed does not read like a photo whose file has moved.
 */
enum class FindCardOpenState {
    /** No open attempt belongs to this card. */
    IDLE,

    /** This card is waiting; it must say so on itself, not elsewhere on screen. */
    OPENING,

    SOURCE_UNAVAILABLE,

    COULD_NOT_OPEN,
}
