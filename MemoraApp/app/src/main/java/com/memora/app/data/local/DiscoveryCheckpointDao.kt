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

    @Query("SELECT * FROM discovery_checkpoints ORDER BY source_id ASC")
    suspend fun findAll(): List<DiscoveryCheckpointEntity>

    @Query("SELECT COUNT(*) FROM discovery_checkpoints")
    suspend fun count(): Int

    @Query("DELETE FROM discovery_checkpoints WHERE source_id = :sourceId")
    suspend fun delete(sourceId: String)
}
