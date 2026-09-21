package com.memora.app.application.find

import com.memora.app.domain.asset.AssetIdentity

/**
 * Presentation filter for user-hidden Find identities.
 *
 * Canonical Recall still ranked these hits. Hide never changes ranking,
 * candidate generation, or Memory integrity.
 */
object FindHiddenPolicy {
    fun <T> visible(
        hits: List<T>,
        hidden: Set<AssetIdentity>,
        identityOf: (T) -> AssetIdentity,
    ): List<T> = hits.filter { identityOf(it) !in hidden }

    fun rankedHitsAreAllHidden(rankedCount: Int, visibleCount: Int): Boolean =
        rankedCount > 0 && visibleCount == 0

    /**
     * Hidden identities that this ranked page actually matched.
     * Idle Find has no ranked identities, so the restore list stays off the home.
     */
    fun <T> relevant(
        hidden: List<T>,
        ranked: Set<AssetIdentity>,
        identityOf: (T) -> AssetIdentity,
    ): List<T> {
        if (hidden.isEmpty() || ranked.isEmpty()) return emptyList()
        return hidden.filter { identityOf(it) in ranked }
    }
}
