package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoOcrExtractionDao {
    @Query(
        """
        SELECT * FROM photo_ocr_extractions
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
    ): PhotoOcrExtractionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhotoOcrExtractionEntity)

    @Query(
        """
        SELECT COUNT(*) FROM photo_ocr_extractions AS e
        INNER JOIN assets AS a
          ON a.source_id = e.source_id
         AND a.source_asset_key = e.source_asset_key
         AND a.fingerprint = e.fingerprint
        WHERE e.source_id = :sourceId
          AND e.schema_version = :schemaVersion
          AND a.asset_type = 'PHOTO'
        """,
    )
    suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int

    @Query(
        """
        SELECT e.source_id, e.source_asset_key, e.fingerprint, e.schema_version,
               e.full_text, a.display_name
        FROM photo_ocr_extractions AS e
        INNER JOIN assets AS a
          ON a.source_id = e.source_id
         AND a.source_asset_key = e.source_asset_key
         AND a.fingerprint = e.fingerprint
        WHERE e.schema_version = :schemaVersion
          AND a.asset_type = 'PHOTO'
          AND e.full_text != ''
          AND e.full_text LIKE '%' || :escapedNeedle || '%' ESCAPE '\'
        ORDER BY CASE WHEN a.display_name IS NULL THEN 1 ELSE 0 END,
                 a.display_name ASC, e.source_asset_key ASC
        LIMIT :limit
        """,
    )
    suspend fun searchCurrentOcrText(
        escapedNeedle: String,
        schemaVersion: String,
        limit: Int,
    ): List<PhotoOcrExtractionSearchRow>

    @Query(
        """
        SELECT COUNT(*) AS photo_count
        FROM photo_ocr_extractions AS e
        INNER JOIN assets AS a
          ON a.source_id = e.source_id
         AND a.source_asset_key = e.source_asset_key
         AND a.fingerprint = e.fingerprint
        WHERE e.schema_version = :schemaVersion
          AND a.asset_type = 'PHOTO'
          AND e.full_text != ''
        """,
    )
    suspend fun countCurrentSearchableCorpus(schemaVersion: String): PhotoOcrSearchableCorpusCounts

    @Query(
        """
        SELECT COUNT(*) FROM photo_ocr_extractions AS e
        INNER JOIN assets AS a
          ON a.source_id = e.source_id
         AND a.source_asset_key = e.source_asset_key
         AND a.fingerprint = e.fingerprint
        WHERE e.schema_version = :schemaVersion
          AND a.asset_type = 'PHOTO'
          AND e.full_text != ''
        """,
    )
    suspend fun countCurrentSearchablePhotos(schemaVersion: String): Int

    @Query("DELETE FROM photo_ocr_extractions")
    suspend fun deleteAll()
}
