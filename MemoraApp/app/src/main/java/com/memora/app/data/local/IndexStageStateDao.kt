package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface IndexStageStateDao {
    @Query(
        """
        SELECT * FROM index_stage_states
        WHERE source_id = :sourceId
          AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint
          AND stage = :stage
          AND derivation_id = :derivationId
        LIMIT 1
        """,
    )
    suspend fun find(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        stage: String,
        derivationId: String,
    ): IndexStageStateEntity?

    @Query(
        """
        SELECT * FROM index_stage_states
        WHERE source_id = :sourceId
          AND source_asset_key = :sourceAssetKey
          AND stage = :stage
        ORDER BY updated_at_epoch_ms DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestForStage(
        sourceId: String,
        sourceAssetKey: String,
        stage: String,
    ): IndexStageStateEntity?

    @Upsert
    suspend fun upsert(entity: IndexStageStateEntity)

    @Query(
        """
        DELETE FROM index_stage_states
        WHERE source_id = :sourceId
          AND source_asset_key = :sourceAssetKey
        """,
    )
    suspend fun deleteForAsset(sourceId: String, sourceAssetKey: String)

    @Query("DELETE FROM index_stage_states")
    suspend fun deleteAll()
}
