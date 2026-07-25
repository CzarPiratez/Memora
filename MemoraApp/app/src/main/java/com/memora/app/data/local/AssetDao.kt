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

    @Query("SELECT * FROM assets ORDER BY source_id ASC, source_asset_key ASC")
    suspend fun findAll(): List<AssetEntity>

    @Query(
        """
        SELECT * FROM assets
        WHERE source_id = :sourceId AND asset_type = :assetType
        ORDER BY source_asset_key ASC
        LIMIT 1
        """,
    )
    suspend fun findFirstBySourceAndType(sourceId: String, assetType: String): AssetEntity?

    @Query(
        """
        SELECT COUNT(*) FROM assets
        WHERE source_id = :sourceId AND asset_type = :assetType
        """,
    )
    suspend fun countBySourceAndType(sourceId: String, assetType: String): Int

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun count(): Int
}
