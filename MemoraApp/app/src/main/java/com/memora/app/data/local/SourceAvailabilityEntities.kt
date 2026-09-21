package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/**
 * Last confirmed Open observation for one Asset identity.
 *
 * Not evidence and not a Memory integrity rewrite. Keyed like note open
 * targets: source identity only, so a restored file with the same identity
 * can become reachable again without a reindex.
 */
@Entity(
    tableName = "source_availability_observations",
    primaryKeys = ["source_id", "source_asset_key"],
)
data class SourceAvailabilityEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "cause") val cause: String,
    @ColumnInfo(name = "observed_at_epoch_millis") val observedAtEpochMillis: Long,
)

@Dao
interface SourceAvailabilityDao {
    @Query(
        """
        SELECT * FROM source_availability_observations
        WHERE source_id = :sourceId AND source_asset_key = :sourceAssetKey
        LIMIT 1
        """,
    )
    suspend fun find(sourceId: String, sourceAssetKey: String): SourceAvailabilityEntity?

    @Query(
        """
        SELECT * FROM source_availability_observations
        WHERE source_id IN (:sourceIds)
        """,
    )
    suspend fun listForSources(sourceIds: List<String>): List<SourceAvailabilityEntity>

    @Upsert(entity = SourceAvailabilityEntity::class)
    suspend fun upsert(entity: SourceAvailabilityEntity)

    @Query("SELECT COUNT(*) FROM source_availability_observations")
    suspend fun count(): Int
}
