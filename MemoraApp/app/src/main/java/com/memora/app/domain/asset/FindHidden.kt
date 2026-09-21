package com.memora.app.domain.asset

import java.time.Instant

/**
 * User-initiated Find visibility for one Asset identity.
 *
 * Orthogonal to Memory integrity and to source availability. Hide does not
 * delete the original, the Memory, or the Why. It is not CR-08 (cascade
 * erase with proof). Ranking never reads this store.
 */
data class FindHiddenItem(
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
    val label: String,
    val hiddenAt: Instant,
) {
    init {
        require(label.isNotBlank()) { "A hidden Find item needs the label the person saw." }
    }

    fun asIdentity(): AssetIdentity = AssetIdentity(sourceId, sourceAssetKey)
}

/**
 * Durable hide/show-again preference. Never consulted to answer a query —
 * only to omit already-ranked Canonical Recall hits from the list.
 */
interface FindHiddenStore {
    suspend fun hide(item: FindHiddenItem)

    suspend fun showAgain(identity: AssetIdentity)

    suspend fun listAll(): List<FindHiddenItem>

    suspend fun hiddenIdentities(
        among: Collection<AssetIdentity>,
    ): Set<AssetIdentity>
}
