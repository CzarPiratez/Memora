package com.memora.app.domain.notes

/**
 * Where a saved note page opens, learned at index time.
 *
 * Measured on device before this existed: resolving these two URLs through
 * Microsoft Graph cost 6.1–7.0s on **every** Open original tap, on a warm
 * session and a healthy connection. Graph returns them on the same page
 * resource discovery already reads, so holding on to them turns that round trip
 * into a local lookup and lets Open original work with no network — which is
 * the behaviour a local-first product should have.
 *
 * This is derived data. It is re-fetchable from Graph, so it is not carried
 * across the plaintext-to-encrypted database migration and does not survive
 * Clear UNFYND index, exactly like every other derived table.
 */
data class NotePageOpenTarget(
    val sourceId: String,
    val sourceAssetKey: String,
    val clientUrl: String?,
    val webUrl: String?,
) {
    init {
        require(sourceId.isNotBlank()) { "A note open target needs a source id." }
        require(sourceAssetKey.isNotBlank()) { "A note open target needs a page id." }
        require(!clientUrl.isNullOrBlank() || !webUrl.isNullOrBlank()) {
            "A note open target needs a client or web URL."
        }
    }
}

/** Local store for [NotePageOpenTarget]. Never reached during search. */
interface NotePageOpenTargetRepository {
    suspend fun find(sourceId: String, sourceAssetKey: String): NotePageOpenTarget?

    /**
     * Records the newest known targets for a page. A page moved between
     * sections gets new URLs, so the latest write wins.
     */
    suspend fun save(target: NotePageOpenTarget)
}
