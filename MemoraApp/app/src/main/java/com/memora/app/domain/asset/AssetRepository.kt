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
}
