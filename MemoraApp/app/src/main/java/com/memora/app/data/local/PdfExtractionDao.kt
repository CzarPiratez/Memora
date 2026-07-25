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

    /**
     * Keyword match over page text for extractions whose fingerprint still matches
     * the Asset row (ADR-022 current version only).
     */
    @Query(
        """
        SELECT
            pages.source_id AS source_id,
            pages.source_asset_key AS source_asset_key,
            pages.fingerprint AS fingerprint,
            pages.schema_version AS schema_version,
            pages.page_number AS page_number,
            pages.page_text AS page_text,
            headers.title AS title,
            assets.display_name AS display_name
        FROM pdf_extraction_pages AS pages
        INNER JOIN pdf_extractions AS headers
            ON headers.source_id = pages.source_id
            AND headers.source_asset_key = pages.source_asset_key
            AND headers.fingerprint = pages.fingerprint
            AND headers.schema_version = pages.schema_version
        INNER JOIN assets AS assets
            ON assets.source_id = pages.source_id
            AND assets.source_asset_key = pages.source_asset_key
            AND assets.fingerprint = pages.fingerprint
        WHERE pages.schema_version = :schemaVersion
          AND pages.page_text LIKE '%' || :escapedNeedle || '%' ESCAPE '\'
        ORDER BY
            CASE WHEN assets.display_name IS NULL THEN 1 ELSE 0 END,
            assets.display_name ASC,
            pages.page_number ASC
        LIMIT :limit
        """,
    )
    suspend fun searchCurrentPages(
        escapedNeedle: String,
        schemaVersion: String,
        limit: Int,
    ): List<PdfExtractionPageSearchRow>

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
