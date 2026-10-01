package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MemoryEmbeddingDao {
    @Query(
        """
        SELECT * FROM memory_embeddings
        WHERE revision_id = :revisionId
          AND model_id = :modelId
          AND model_version = :modelVersion
        LIMIT 1
        """,
    )
    suspend fun find(
        revisionId: String,
        modelId: String,
        modelVersion: String,
    ): MemoryEmbeddingEntity?

    @Upsert
    suspend fun upsert(entity: MemoryEmbeddingEntity)

    @Query(
        """
        SELECT COUNT(*) FROM memory_embeddings
        WHERE model_id = :modelId
          AND model_version = :modelVersion
        """,
    )
    suspend fun countForModel(modelId: String, modelVersion: String): Int

    @Query(
        """
        SELECT * FROM memory_embeddings
        WHERE model_id = :modelId
          AND model_version = :modelVersion
        ORDER BY created_at_epoch_ms DESC
        """,
    )
    suspend fun listForModel(modelId: String, modelVersion: String): List<MemoryEmbeddingEntity>

    @Query(
        """
        DELETE FROM memory_embeddings
        WHERE model_id = :modelId
          AND model_version = :modelVersion
        """,
    )
    suspend fun deleteForModel(modelId: String, modelVersion: String): Int
}
