package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScreenshotOcrExtractionDao {
    @Query(
        """
        SELECT * FROM screenshot_ocr_extractions
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
    ): ScreenshotOcrExtractionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScreenshotOcrExtractionEntity)

    @Query(
        """
        SELECT COUNT(*) FROM screenshot_ocr_extractions AS e
        INNER JOIN assets AS a
          ON a.source_id = e.source_id
         AND a.source_asset_key = e.source_asset_key
         AND a.fingerprint = e.fingerprint
        WHERE e.source_id = :sourceId
          AND e.schema_version = :schemaVersion
        """,
    )
    suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int

    /**
     * Keyword match over OCR text whose fingerprint still matches the Asset
     * (ADR-022). Empty OCR text is not searchable.
     */
    @Query(
        """
        SELECT
            e.source_id AS source_id,
            e.source_asset_key AS source_asset_key,
            e.fingerprint AS fingerprint,
            e.schema_version AS schema_version,
            e.full_text AS full_text,
            a.display_name AS display_name
        FROM screenshot_ocr_extractions AS e
        INNER JOIN assets AS a
            ON a.source_id = e.source_id
            AND a.source_asset_key = e.source_asset_key
            AND a.fingerprint = e.fingerprint
        WHERE e.schema_version = :schemaVersion
          AND a.asset_type = 'SCREENSHOT'
          AND e.full_text != ''
          AND e.full_text LIKE '%' || :escapedNeedle || '%' ESCAPE '\'
        ORDER BY
            CASE WHEN a.display_name IS NULL THEN 1 ELSE 0 END,
            a.display_name ASC,
            e.source_asset_key ASC
        LIMIT :limit
        """,
    )
    suspend fun searchCurrentOcrText(
        escapedNeedle: String,
        schemaVersion: String,
        limit: Int,
    ): List<ScreenshotOcrExtractionSearchRow>

    /**
     * Count of current-fingerprint screenshots with non-empty OCR text
     * (ADR-022). Zero means empty corpus.
     */
    @Query(
        """
        SELECT COUNT(*) AS screenshot_count
        FROM screenshot_ocr_extractions AS e
        INNER JOIN assets AS a
            ON a.source_id = e.source_id
            AND a.source_asset_key = e.source_asset_key
            AND a.fingerprint = e.fingerprint
        WHERE e.schema_version = :schemaVersion
          AND a.asset_type = 'SCREENSHOT'
          AND e.full_text != ''
        """,
    )
    suspend fun countCurrentSearchableCorpus(schemaVersion: String): ScreenshotOcrSearchableCorpusCounts

    @Query(
        """
        SELECT COUNT(*)
        FROM screenshot_ocr_extractions AS e
        INNER JOIN assets AS a
            ON a.source_id = e.source_id
            AND a.source_asset_key = e.source_asset_key
            AND a.fingerprint = e.fingerprint
        WHERE e.schema_version = :schemaVersion
          AND a.asset_type = 'SCREENSHOT'
          AND e.full_text != ''
        """,
    )
    suspend fun countCurrentSearchableScreenshots(schemaVersion: String): Int

    @Query("DELETE FROM screenshot_ocr_extractions")
    suspend fun deleteAll()
}
