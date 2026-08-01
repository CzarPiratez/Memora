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
}
