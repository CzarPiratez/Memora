package com.memora.app.data.local

import androidx.room.withTransaction
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.IndexingState
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryPageStore
import java.time.Clock

/**
 * Atomically persists discovered Asset placeholders and the source cursor that produced
 * them. Existing unchanged assets retain their indexing state; changed fingerprints
 * start the indexing lifecycle again from DISCOVERED.
 */
class RoomDiscoveryPageStore(
    private val database: () -> MemoraDatabase,
    private val clock: Clock = Clock.systemUTC(),
) : DiscoveryPageStore {
    override suspend fun save(page: DiscoveryPage) {
        val db = database()
        db.withTransaction {
            page.assets.forEach { asset ->
                val existing = db.assetDao().find(
                    asset.identity.sourceId.value,
                    asset.identity.sourceAssetKey.value,
                )
                val record = when {
                    existing == null -> AssetIndexRecord(asset, IndexingState.discovered)
                    existing.fingerprint == asset.fingerprint.value -> existing.toDomain().copy(asset = asset)
                    else -> AssetIndexRecord(asset, IndexingState.discovered)
                }
                db.assetDao().upsert(record.toEntity())
            }
            db.discoveryCheckpointDao().upsert(page.checkpoint.toEntity(clock.instant()))
        }
    }
}
