package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PdfPageEmbeddingDao {
    @Query(
        """
        SELECT * FROM pdf_page_embeddings
        WHERE revision_id = :revisionId
          AND page_number = :pageNumber
          AND model_id = :modelId
          AND model_version = :modelVersion
        LIMIT 1
        """,
    )
    suspend fun find(
        revisionId: String,
        pageNumber: Int,
        modelId: String,
        modelVersion: String,
    ): PdfPageEmbeddingEntity?

    @Upsert
    suspend fun upsert(entity: PdfPageEmbeddingEntity)

    @Query(
        """
        SELECT COUNT(*) FROM pdf_page_embeddings
        WHERE model_id = :modelId
          AND model_version = :modelVersion
        """,
    )
    suspend fun countForModel(modelId: String, modelVersion: String): Int

    @Query(
        """
        SELECT * FROM pdf_page_embeddings
        WHERE model_id = :modelId
          AND model_version = :modelVersion
        ORDER BY created_at_epoch_ms DESC
        """,
    )
    suspend fun listForModel(modelId: String, modelVersion: String): List<PdfPageEmbeddingEntity>
}
