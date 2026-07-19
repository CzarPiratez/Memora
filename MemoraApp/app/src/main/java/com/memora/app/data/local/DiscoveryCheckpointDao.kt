package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DiscoveryCheckpointDao {
    @Upsert
    suspend fun upsert(checkpoint: DiscoveryCheckpointEntity)

    @Query(
        """
        SELECT * FROM discovery_checkpoints
        WHERE source_id = :sourceId
        LIMIT 1
        """,
    )
    suspend fun find(sourceId: String): DiscoveryCheckpointEntity?
}
