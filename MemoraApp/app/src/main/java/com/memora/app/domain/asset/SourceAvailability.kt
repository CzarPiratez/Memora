package com.memora.app.domain.asset

import java.time.Instant

/**
 * Last-known reachability of an original Asset. Orthogonal to Memory integrity.
 *
 * A Memory can be READY while the original is unreachable. Find still returns
 * that Memory (I16 / R1). This record only tells the truth about Open. It is
 * never a ranking input and never silently deletes derived knowledge.
 *
 * Slice 1 records confirmed Open outcomes and Open-class thumbnail reopens.
 * Discovery absence (a complete source scan that no longer lists the Asset)
 * is a later slice — incomplete pages must not tombstone.
 */
enum class SourceAvailabilityStatus {
    /** No confirmed Open observation yet. Search must not probe the original. */
    UNKNOWN,

    /** Last confirmed Open (or equivalent reopen) succeeded. */
    REACHABLE,

    /**
     * Last confirmed Open could not reach the original. May be deletion, a
     * move, a revoked grant, or a disconnected folder — Open's current
     * outcomes collapse those. Copy must not claim a single cause.
     */
    UNREACHABLE,
}

enum class SourceAvailabilityCause {
    OPEN_SUCCEEDED,
    OPEN_FAILED_UNREACHABLE,
}

/**
 * One durable observation about whether UNFYND can reopen an original.
 *
 * Keyed by Asset identity, not Memory revision: a new understanding revision
 * does not revive a deleted file, and a restored file should reopen the same
 * identity.
 */
data class SourceAvailabilityObservation(
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
    val status: SourceAvailabilityStatus,
    val cause: SourceAvailabilityCause,
    val observedAt: Instant,
) {
    init {
        require(status != SourceAvailabilityStatus.UNKNOWN) {
            "Unknown availability is the absence of a row, not a stored observation."
        }
        require(
            (status == SourceAvailabilityStatus.REACHABLE) ==
                (cause == SourceAvailabilityCause.OPEN_SUCCEEDED),
        ) {
            "Reachable observations come from a successful Open; unreachable from a failed one."
        }
    }
}

/**
 * Durable store for [SourceAvailabilityObservation]. Never consulted to answer
 * a query — only to present Open honesty on already-ranked Canonical Recall hits.
 */
interface SourceAvailabilityStore {
    suspend fun find(
        sourceId: SourceId,
        sourceAssetKey: SourceAssetKey,
    ): SourceAvailabilityObservation?

    suspend fun findAll(
        identities: Collection<AssetIdentity>,
    ): Map<AssetIdentity, SourceAvailabilityObservation>

    suspend fun save(observation: SourceAvailabilityObservation)
}
