package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MemoryAssemblySkipDao {
    @Upsert
    suspend fun upsert(entity: MemoryAssemblySkipEntity)

    @Query(
        """
        DELETE FROM memory_assembly_skips
        WHERE source_id = :sourceId
          AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint
        """,
    )
    suspend fun deleteForAsset(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
    )
}
