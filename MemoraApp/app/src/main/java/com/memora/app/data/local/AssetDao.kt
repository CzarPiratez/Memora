package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AssetDao {
    @Upsert
    suspend fun upsert(asset: AssetEntity)

    @Query(
        """
        SELECT * FROM assets
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
        LIMIT 1
        """,
    )
    suspend fun find(sourceId: String, sourceAssetKey: String): AssetEntity?
}
