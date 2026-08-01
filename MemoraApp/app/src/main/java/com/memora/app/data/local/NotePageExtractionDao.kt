package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotePageExtractionDao {
    @Query(
        """
        SELECT * FROM note_page_extractions
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
    ): NotePageExtractionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NotePageExtractionEntity)

    @Query(
        """
        SELECT COUNT(*) FROM note_page_extractions AS e
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
     * Keyword match over note page text whose fingerprint still matches the Asset
     * (ADR-022). Empty extract text is not searchable.
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
        FROM note_page_extractions AS e
        INNER JOIN assets AS a
            ON a.source_id = e.source_id
            AND a.source_asset_key = e.source_asset_key
            AND a.fingerprint = e.fingerprint
        WHERE e.schema_version = :schemaVersion
          AND a.asset_type = 'NOTE'
          AND e.full_text != ''
          AND e.full_text LIKE '%' || :escapedNeedle || '%' ESCAPE '\'
        ORDER BY
            CASE WHEN a.display_name IS NULL THEN 1 ELSE 0 END,
            a.display_name ASC,
            e.source_asset_key ASC
        LIMIT :limit
        """,
    )
    suspend fun searchCurrentNoteText(
        escapedNeedle: String,
        schemaVersion: String,
        limit: Int,
    ): List<NotePageExtractionSearchRow>

    @Query(
        """
        SELECT COUNT(*) AS note_count
        FROM note_page_extractions AS e
        INNER JOIN assets AS a
            ON a.source_id = e.source_id
            AND a.source_asset_key = e.source_asset_key
            AND a.fingerprint = e.fingerprint
        WHERE e.schema_version = :schemaVersion
          AND a.asset_type = 'NOTE'
          AND e.full_text != ''
        """,
    )
    suspend fun countCurrentSearchableCorpus(schemaVersion: String): NotePageSearchableCorpusCounts

    @Query(
        """
        SELECT COUNT(*)
        FROM note_page_extractions AS e
        INNER JOIN assets AS a
            ON a.source_id = e.source_id
            AND a.source_asset_key = e.source_asset_key
            AND a.fingerprint = e.fingerprint
        WHERE e.schema_version = :schemaVersion
          AND a.asset_type = 'NOTE'
          AND e.full_text != ''
        """,
    )
    suspend fun countCurrentSearchableNotes(schemaVersion: String): Int
}
