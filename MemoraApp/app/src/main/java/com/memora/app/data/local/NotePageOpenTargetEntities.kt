package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/**
 * Where a saved OneNote page opens, read during discovery from the same Graph
 * page resource the rest of the placeholder comes from (I5).
 *
 * Deliberately **not** keyed on fingerprint, unlike the extraction tables. Those
 * key on it so superseded provenance can coexist (ADR-022); an open target is
 * not evidence and carries no claim about page content, so an edited page should
 * keep opening rather than silently lose its link until re-extraction.
 */
@Entity(
    tableName = "note_page_open_targets",
    primaryKeys = ["source_id", "source_asset_key"],
)
data class NotePageOpenTargetEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "client_url") val clientUrl: String?,
    @ColumnInfo(name = "web_url") val webUrl: String?,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)

@Dao
interface NotePageOpenTargetDao {
    @Query(
        """
        SELECT * FROM note_page_open_targets
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
        LIMIT 1
        """,
    )
    suspend fun find(sourceId: String, sourceAssetKey: String): NotePageOpenTargetEntity?

    @Upsert(entity = NotePageOpenTargetEntity::class)
    suspend fun upsert(entity: NotePageOpenTargetEntity)

    @Upsert(entity = NotePageOpenTargetEntity::class)
    suspend fun upsertAll(entities: List<NotePageOpenTargetEntity>)

    @Query("SELECT COUNT(*) FROM note_page_open_targets")
    suspend fun count(): Int
}
