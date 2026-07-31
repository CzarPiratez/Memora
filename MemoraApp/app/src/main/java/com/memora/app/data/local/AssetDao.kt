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

    /**
     * Next PDF for local reading: no current-fingerprint extraction for [schemaVersion],
     * ordered after [afterSourceAssetKey] (exclusive). Empty [afterSourceAssetKey] starts
     * from the beginning.
     */
    @Query(
        """
        SELECT * FROM assets
        WHERE source_id = :sourceId
          AND asset_type = :assetType
          AND (:afterSourceAssetKey = '' OR source_asset_key > :afterSourceAssetKey)
          AND NOT EXISTS (
            SELECT 1 FROM pdf_extractions AS extractions
            WHERE extractions.source_id = assets.source_id
              AND extractions.source_asset_key = assets.source_asset_key
              AND extractions.fingerprint = assets.fingerprint
              AND extractions.schema_version = :schemaVersion
          )
        ORDER BY source_asset_key ASC
        LIMIT 1
        """,
    )
    suspend fun findNextPdfPendingLocalReading(
        sourceId: String,
        assetType: String,
        schemaVersion: String,
        afterSourceAssetKey: String,
    ): AssetEntity?

    /**
     * Next PHOTO/SCREENSHOT still needing EXIF extract for [schemaVersion], ordered
     * after [afterSourceAssetKey] (exclusive). Empty [afterSourceAssetKey] starts first.
     */
    @Query(
        """
        SELECT * FROM assets
        WHERE source_id = :sourceId
          AND asset_type IN ('PHOTO', 'SCREENSHOT')
          AND (:afterSourceAssetKey = '' OR source_asset_key > :afterSourceAssetKey)
          AND NOT EXISTS (
            SELECT 1 FROM image_exif_extractions AS extractions
            WHERE extractions.source_id = assets.source_id
              AND extractions.source_asset_key = assets.source_asset_key
              AND extractions.fingerprint = assets.fingerprint
              AND extractions.schema_version = :schemaVersion
          )
        ORDER BY source_asset_key ASC
        LIMIT 1
        """,
    )
    suspend fun findNextImagePendingExifExtract(
        sourceId: String,
        schemaVersion: String,
        afterSourceAssetKey: String,
    ): AssetEntity?

    /**
     * Next SCREENSHOT still needing OCR extract for [schemaVersion], ordered
     * after [afterSourceAssetKey] (exclusive). Empty [afterSourceAssetKey] starts first.
     */
    @Query(
        """
        SELECT * FROM assets
        WHERE source_id = :sourceId
          AND asset_type = 'SCREENSHOT'
          AND (:afterSourceAssetKey = '' OR source_asset_key > :afterSourceAssetKey)
          AND NOT EXISTS (
            SELECT 1 FROM screenshot_ocr_extractions AS extractions
            WHERE extractions.source_id = assets.source_id
              AND extractions.source_asset_key = assets.source_asset_key
              AND extractions.fingerprint = assets.fingerprint
              AND extractions.schema_version = :schemaVersion
          )
        ORDER BY source_asset_key ASC
        LIMIT 1
        """,
    )
    suspend fun findNextScreenshotPendingOcrExtract(
        sourceId: String,
        schemaVersion: String,
        afterSourceAssetKey: String,
    ): AssetEntity?

    @Query(
        """
        SELECT * FROM assets
        WHERE source_id = :sourceId
          AND asset_type = 'PHOTO'
          AND (:afterSourceAssetKey = '' OR source_asset_key > :afterSourceAssetKey)
          AND NOT EXISTS (
            SELECT 1 FROM photo_ocr_extractions AS extractions
            WHERE extractions.source_id = assets.source_id
              AND extractions.source_asset_key = assets.source_asset_key
              AND extractions.fingerprint = assets.fingerprint
              AND extractions.schema_version = :schemaVersion
          )
        ORDER BY source_asset_key ASC
        LIMIT 1
        """,
    )
    suspend fun findNextPhotoPendingOcrExtract(
        sourceId: String,
        schemaVersion: String,
        afterSourceAssetKey: String,
    ): AssetEntity?

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun count(): Int
}
