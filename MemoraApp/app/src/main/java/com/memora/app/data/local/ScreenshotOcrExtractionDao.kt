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

    @Query("DELETE FROM screenshot_ocr_extractions")
    suspend fun deleteAll()
}
