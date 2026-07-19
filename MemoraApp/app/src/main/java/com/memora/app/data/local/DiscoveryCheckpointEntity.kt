package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCursor
import java.time.Instant

/** Room representation of one source's last durable discovery checkpoint. */
@Entity(tableName = "discovery_checkpoints")
data class DiscoveryCheckpointEntity(
    @PrimaryKey
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "cursor_value") val cursorValue: String,
    @ColumnInfo(name = "saved_at_epoch_millis") val savedAtEpochMillis: Long,
)

internal fun DiscoveryCursor.toEntity(savedAt: Instant): DiscoveryCheckpointEntity =
    DiscoveryCheckpointEntity(
        sourceId = sourceId.value,
        cursorValue = value,
        savedAtEpochMillis = savedAt.toEpochMilli(),
    )

internal fun DiscoveryCheckpointEntity.toDomain(): DiscoveryCursor = DiscoveryCursor(
    sourceId = SourceId(sourceId),
    value = cursorValue,
)
