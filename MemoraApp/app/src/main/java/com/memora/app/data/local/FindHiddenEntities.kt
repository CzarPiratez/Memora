package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/**
 * User-hidden Find identity. Not a Memory delete and not CR-08.
 */
@Entity(
    tableName = "find_hidden_identities",
    primaryKeys = ["source_id", "source_asset_key"],
)
data class FindHiddenEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "hidden_at_epoch_millis") val hiddenAtEpochMillis: Long,
)

@Dao
interface FindHiddenDao {
    @Query(
        """
        SELECT * FROM find_hidden_identities
        ORDER BY hidden_at_epoch_millis DESC
        """,
    )
    suspend fun listAll(): List<FindHiddenEntity>

    @Query(
        """
        SELECT * FROM find_hidden_identities
        WHERE source_id IN (:sourceIds)
        """,
    )
    suspend fun listForSources(sourceIds: List<String>): List<FindHiddenEntity>

    @Upsert(entity = FindHiddenEntity::class)
    suspend fun upsert(entity: FindHiddenEntity)

    @Query(
        """
        DELETE FROM find_hidden_identities
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
        """,
    )
    suspend fun delete(sourceId: String, sourceAssetKey: String)

    @Query("SELECT COUNT(*) FROM find_hidden_identities")
    suspend fun count(): Int
}
