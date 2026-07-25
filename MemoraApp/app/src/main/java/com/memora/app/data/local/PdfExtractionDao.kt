package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PdfExtractionDao {
    @Query(
        """
        SELECT * FROM pdf_extractions
        WHERE source_id = :sourceId
          AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint
          AND schema_version = :schemaVersion
        LIMIT 1
        """,
    )
    suspend fun findHeader(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): PdfExtractionEntity?

    @Query(
        """
        SELECT * FROM pdf_extraction_pages
        WHERE source_id = :sourceId
          AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint
          AND schema_version = :schemaVersion
        ORDER BY page_number ASC
        """,
    )
    suspend fun findPages(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): List<PdfExtractionPageEntity>

    @Query(
        """
        SELECT * FROM pdf_extraction_metadata
        WHERE source_id = :sourceId
          AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint
          AND schema_version = :schemaVersion
        """,
    )
    suspend fun findMetadata(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): List<PdfExtractionMetadataEntity>

    @Query(
        """
        SELECT COUNT(*) FROM pdf_extractions
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
        """,
    )
    suspend fun countForAsset(sourceId: String, sourceAssetKey: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHeader(entity: PdfExtractionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPages(entities: List<PdfExtractionPageEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMetadata(entities: List<PdfExtractionMetadataEntity>)

    @Query("DELETE FROM pdf_extractions")
    suspend fun deleteAll()

    @Query(
        """
        DELETE FROM pdf_extractions
        WHERE source_id = :sourceId
          AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint
          AND schema_version = :schemaVersion
        """,
    )
    suspend fun deleteHeader(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): Int

    @Transaction
    suspend fun insertAtomic(
        header: PdfExtractionEntity,
        pages: List<PdfExtractionPageEntity>,
        metadata: List<PdfExtractionMetadataEntity>,
    ) {
        insertHeader(header)
        if (pages.isNotEmpty()) {
            insertPages(pages)
        }
        if (metadata.isNotEmpty()) {
            insertMetadata(metadata)
        }
    }
}
