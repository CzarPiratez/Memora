package com.memora.app.domain.discovery

import com.memora.app.domain.asset.SourceId

/** Durable, source-owned progress used to resume a bounded discovery pass safely. */
interface DiscoveryCheckpointRepository {
    suspend fun save(cursor: DiscoveryCursor)

    suspend fun find(sourceId: SourceId): DiscoveryCursor?
}
