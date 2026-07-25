package com.memora.app.domain.asset

/** The atomic local record that keeps a source Asset and its recoverable state together. */
data class AssetIndexRecord(
    val asset: Asset,
    val indexingState: IndexingState,
)

/** Persistence boundary for source-neutral assets. Implementations must be idempotent. */
interface AssetRepository {
    suspend fun save(record: AssetIndexRecord)

    suspend fun find(identity: AssetIdentity): AssetIndexRecord?

    /** Returns the first stored asset for [sourceId] with [type], or null when none exist. */
    suspend fun findFirstBySourceAndType(sourceId: SourceId, type: AssetType): Asset?
}
