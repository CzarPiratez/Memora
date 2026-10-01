package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MemoryEvidenceEmbeddingDao {
    @Query(
        """
        SELECT * FROM memory_evidence_embeddings
        WHERE revision_id = :revisionId
          AND evidence_id = :evidenceId
          AND model_id = :modelId
          AND model_version = :modelVersion
        LIMIT 1
        """,
    )
    suspend fun find(
        revisionId: String,
        evidenceId: String,
        modelId: String,
        modelVersion: String,
    ): MemoryEvidenceEmbeddingEntity?

    @Upsert
    suspend fun upsert(entity: MemoryEvidenceEmbeddingEntity)

    @Query(
        """
        SELECT COUNT(*) FROM memory_evidence_embeddings
        WHERE model_id = :modelId
          AND model_version = :modelVersion
        """,
    )
    suspend fun countForModel(modelId: String, modelVersion: String): Int

    @Query(
        """
        SELECT * FROM memory_evidence_embeddings
        WHERE model_id = :modelId
          AND model_version = :modelVersion
        ORDER BY created_at_epoch_ms DESC
        """,
    )
    suspend fun listForModel(modelId: String, modelVersion: String): List<MemoryEvidenceEmbeddingEntity>

    @Query(
        """
        DELETE FROM memory_evidence_embeddings
        WHERE model_id = :modelId
          AND model_version = :modelVersion
        """,
    )
    suspend fun deleteForModel(modelId: String, modelVersion: String): Int
}
