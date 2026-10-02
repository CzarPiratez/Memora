package com.memora.app.data.local

import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCheckpointRepository
import com.memora.app.domain.discovery.DiscoveryCursor
import java.time.Clock

/** Room-backed persistence for opaque source scan checkpoints. */
class RoomDiscoveryCheckpointRepository(
    private val checkpointDao: () -> DiscoveryCheckpointDao,
    private val clock: Clock = Clock.systemUTC(),
) : DiscoveryCheckpointRepository {
    override suspend fun save(cursor: DiscoveryCursor) {
        checkpointDao().upsert(cursor.toEntity(clock.instant()))
    }

    override suspend fun find(sourceId: SourceId): DiscoveryCursor? = checkpointDao()
        .find(sourceId.value)
        ?.toDomain()

    override suspend fun isCheckpointCompleted(sourceId: SourceId): Boolean {
        val cursor = find(sourceId) ?: return false
        return runCatching {
            com.memora.app.data.saf.SafPdfDiscoveryCheckpoint.from(cursor).frames.isEmpty()
        }.getOrDefault(false)
    }

    override suspend fun delete(sourceId: SourceId) {
        checkpointDao().delete(sourceId.value)
    }
}
