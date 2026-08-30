package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query

@Dao
interface AssetMemoryFactDao {
    @Query(
        """
        SELECT * FROM assets AS a
        WHERE NOT EXISTS (
            SELECT 1 FROM memories AS m
            WHERE m.source_id = a.source_id
              AND m.source_asset_key = a.source_asset_key
              AND m.fingerprint = a.fingerprint
              AND m.assembly_schema_version = :assemblySchemaVersion
        )
        AND (
            EXISTS (
                SELECT 1 FROM pdf_extraction_pages AS p
                INNER JOIN pdf_extractions AS h
                  ON h.source_id = p.source_id
                 AND h.source_asset_key = p.source_asset_key
                 AND h.fingerprint = p.fingerprint
                 AND h.schema_version = p.schema_version
                WHERE p.source_id = a.source_id
                  AND p.source_asset_key = a.source_asset_key
                  AND p.fingerprint = a.fingerprint
                  AND p.schema_version = :pdfSchemaVersion
                  AND h.integrity = 'VERIFIED'
                  AND TRIM(p.page_text) != ''
            )
            OR EXISTS (
                SELECT 1 FROM pdf_extractions AS h
                WHERE h.source_id = a.source_id
                  AND h.source_asset_key = a.source_asset_key
                  AND h.fingerprint = a.fingerprint
                  AND h.schema_version = :pdfSchemaVersion
                  AND h.integrity = 'VERIFIED'
                  AND h.title IS NOT NULL
                  AND TRIM(h.title) != ''
            )
            OR EXISTS (
                SELECT 1 FROM pdf_extraction_metadata AS md
                INNER JOIN pdf_extractions AS h
                  ON h.source_id = md.source_id
                 AND h.source_asset_key = md.source_asset_key
                 AND h.fingerprint = md.fingerprint
                 AND h.schema_version = md.schema_version
                WHERE md.source_id = a.source_id
                  AND md.source_asset_key = a.source_asset_key
                  AND md.fingerprint = a.fingerprint
                  AND md.schema_version = :pdfSchemaVersion
                  AND h.integrity = 'VERIFIED'
                  AND TRIM(md.metadata_value) != ''
            )
            OR EXISTS (
                SELECT 1 FROM screenshot_ocr_extractions AS o
                WHERE o.source_id = a.source_id
                  AND o.source_asset_key = a.source_asset_key
                  AND o.fingerprint = a.fingerprint
                  AND o.schema_version = :screenshotOcrSchemaVersion
                  AND o.integrity = 'VERIFIED'
                  AND TRIM(o.full_text) != ''
            )
            OR EXISTS (
                SELECT 1 FROM photo_ocr_extractions AS o
                WHERE o.source_id = a.source_id
                  AND o.source_asset_key = a.source_asset_key
                  AND o.fingerprint = a.fingerprint
                  AND o.schema_version = :photoOcrSchemaVersion
                  AND o.integrity = 'VERIFIED'
                  AND TRIM(o.full_text) != ''
            )
            OR EXISTS (
                SELECT 1 FROM image_exif_extractions AS x
                WHERE x.source_id = a.source_id
                  AND x.source_asset_key = a.source_asset_key
                  AND x.fingerprint = a.fingerprint
                  AND x.schema_version = :exifSchemaVersion
                  AND x.integrity = 'VERIFIED'
                  AND (
                    (x.datetime_original IS NOT NULL AND TRIM(x.datetime_original) != '')
                    OR (x.make IS NOT NULL AND TRIM(x.make) != '')
                    OR (x.model IS NOT NULL AND TRIM(x.model) != '')
                  )
            )
            OR EXISTS (
                SELECT 1 FROM note_page_extractions AS n
                WHERE n.source_id = a.source_id
                  AND n.source_asset_key = a.source_asset_key
                  AND n.fingerprint = a.fingerprint
                  AND n.schema_version = :notePageSchemaVersion
                  AND n.integrity = 'VERIFIED'
                  AND TRIM(n.full_text) != ''
            )
        )
        ORDER BY a.source_id, a.source_asset_key
        LIMIT 1
        """,
    )
    suspend fun findNextPendingAsset(
        assemblySchemaVersion: String,
        pdfSchemaVersion: String,
        screenshotOcrSchemaVersion: String,
        photoOcrSchemaVersion: String,
        exifSchemaVersion: String,
        notePageSchemaVersion: String,
    ): AssetEntity?

    @Query(
        """
        SELECT COUNT(*) FROM assets AS a
        WHERE NOT EXISTS (
            SELECT 1 FROM memories AS m
            WHERE m.source_id = a.source_id
              AND m.source_asset_key = a.source_asset_key
              AND m.fingerprint = a.fingerprint
              AND m.assembly_schema_version = :assemblySchemaVersion
        )
        AND (
            EXISTS (
                SELECT 1 FROM pdf_extraction_pages AS p
                INNER JOIN pdf_extractions AS h
                  ON h.source_id = p.source_id
                 AND h.source_asset_key = p.source_asset_key
                 AND h.fingerprint = p.fingerprint
                 AND h.schema_version = p.schema_version
                WHERE p.source_id = a.source_id
                  AND p.source_asset_key = a.source_asset_key
                  AND p.fingerprint = a.fingerprint
                  AND p.schema_version = :pdfSchemaVersion
                  AND h.integrity = 'VERIFIED'
                  AND TRIM(p.page_text) != ''
            )
            OR EXISTS (
                SELECT 1 FROM pdf_extractions AS h
                WHERE h.source_id = a.source_id
                  AND h.source_asset_key = a.source_asset_key
                  AND h.fingerprint = a.fingerprint
                  AND h.schema_version = :pdfSchemaVersion
                  AND h.integrity = 'VERIFIED'
                  AND h.title IS NOT NULL
                  AND TRIM(h.title) != ''
            )
            OR EXISTS (
                SELECT 1 FROM pdf_extraction_metadata AS md
                INNER JOIN pdf_extractions AS h
                  ON h.source_id = md.source_id
                 AND h.source_asset_key = md.source_asset_key
                 AND h.fingerprint = md.fingerprint
                 AND h.schema_version = md.schema_version
                WHERE md.source_id = a.source_id
                  AND md.source_asset_key = a.source_asset_key
                  AND md.fingerprint = a.fingerprint
                  AND md.schema_version = :pdfSchemaVersion
                  AND h.integrity = 'VERIFIED'
                  AND TRIM(md.metadata_value) != ''
            )
            OR EXISTS (
                SELECT 1 FROM screenshot_ocr_extractions AS o
                WHERE o.source_id = a.source_id
                  AND o.source_asset_key = a.source_asset_key
                  AND o.fingerprint = a.fingerprint
                  AND o.schema_version = :screenshotOcrSchemaVersion
                  AND o.integrity = 'VERIFIED'
                  AND TRIM(o.full_text) != ''
            )
            OR EXISTS (
                SELECT 1 FROM photo_ocr_extractions AS o
                WHERE o.source_id = a.source_id
                  AND o.source_asset_key = a.source_asset_key
                  AND o.fingerprint = a.fingerprint
                  AND o.schema_version = :photoOcrSchemaVersion
                  AND o.integrity = 'VERIFIED'
                  AND TRIM(o.full_text) != ''
            )
            OR EXISTS (
                SELECT 1 FROM image_exif_extractions AS x
                WHERE x.source_id = a.source_id
                  AND x.source_asset_key = a.source_asset_key
                  AND x.fingerprint = a.fingerprint
                  AND x.schema_version = :exifSchemaVersion
                  AND x.integrity = 'VERIFIED'
                  AND (
                    (x.datetime_original IS NOT NULL AND TRIM(x.datetime_original) != '')
                    OR (x.make IS NOT NULL AND TRIM(x.make) != '')
                    OR (x.model IS NOT NULL AND TRIM(x.model) != '')
                  )
            )
            OR EXISTS (
                SELECT 1 FROM note_page_extractions AS n
                WHERE n.source_id = a.source_id
                  AND n.source_asset_key = a.source_asset_key
                  AND n.fingerprint = a.fingerprint
                  AND n.schema_version = :notePageSchemaVersion
                  AND n.integrity = 'VERIFIED'
                  AND TRIM(n.full_text) != ''
            )
        )
        """,
    )
    suspend fun countPendingAssembly(
        assemblySchemaVersion: String,
        pdfSchemaVersion: String,
        screenshotOcrSchemaVersion: String,
        photoOcrSchemaVersion: String,
        exifSchemaVersion: String,
        notePageSchemaVersion: String,
    ): Int

    @Query(
        """
        SELECT p.* FROM pdf_extraction_pages AS p
        INNER JOIN pdf_extractions AS h
          ON h.source_id = p.source_id AND h.source_asset_key = p.source_asset_key
         AND h.fingerprint = p.fingerprint AND h.schema_version = p.schema_version
        WHERE p.source_id = :sourceId AND p.source_asset_key = :sourceAssetKey
          AND p.fingerprint = :fingerprint AND p.schema_version = :schemaVersion
          AND h.integrity = 'VERIFIED' AND TRIM(p.page_text) != ''
        ORDER BY p.page_number
        """,
    )
    suspend fun findPdfPages(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): List<PdfExtractionPageEntity>

    @Query(
        """
        SELECT * FROM pdf_extractions
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint AND schema_version = :schemaVersion
          AND integrity = 'VERIFIED'
        LIMIT 1
        """,
    )
    suspend fun findPdfHeader(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): PdfExtractionEntity?

    @Query(
        """
        SELECT md.* FROM pdf_extraction_metadata AS md
        INNER JOIN pdf_extractions AS h
          ON h.source_id = md.source_id AND h.source_asset_key = md.source_asset_key
         AND h.fingerprint = md.fingerprint AND h.schema_version = md.schema_version
        WHERE md.source_id = :sourceId AND md.source_asset_key = :sourceAssetKey
          AND md.fingerprint = :fingerprint AND md.schema_version = :schemaVersion
          AND h.integrity = 'VERIFIED' AND TRIM(md.metadata_value) != ''
        ORDER BY md.metadata_name
        """,
    )
    suspend fun findPdfMetadata(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): List<PdfExtractionMetadataEntity>

    @Query(
        """
        SELECT * FROM screenshot_ocr_extractions
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint AND schema_version = :schemaVersion
          AND integrity = 'VERIFIED'
        LIMIT 1
        """,
    )
    suspend fun findScreenshotOcr(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): ScreenshotOcrExtractionEntity?

    @Query(
        """
        SELECT * FROM photo_ocr_extractions
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint AND schema_version = :schemaVersion
          AND integrity = 'VERIFIED'
        LIMIT 1
        """,
    )
    suspend fun findPhotoOcr(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): PhotoOcrExtractionEntity?

    @Query(
        """
        SELECT * FROM image_exif_extractions
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint AND schema_version = :schemaVersion
          AND integrity = 'VERIFIED'
        LIMIT 1
        """,
    )
    suspend fun findExif(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): ImageExifExtractionEntity?

    @Query(
        """
        SELECT * FROM note_page_extractions
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint AND schema_version = :schemaVersion
          AND integrity = 'VERIFIED'
          AND TRIM(full_text) != ''
        LIMIT 1
        """,
    )
    suspend fun findNotePage(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): NotePageExtractionEntity?
}
