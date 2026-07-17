package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.IndexingFailure
import com.memora.app.domain.asset.IndexingState
import com.memora.app.domain.asset.IndexingStatus
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Instant

/** Room representation of the atomic Asset plus indexing-state aggregate. */
@Entity(
    tableName = "assets",
    primaryKeys = ["source_id", "source_asset_key"],
    indices = [
        Index(value = ["fingerprint"]),
        Index(value = ["indexing_status"]),
    ],
)
data class AssetEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "asset_type") val assetType: String,
    @ColumnInfo(name = "location") val location: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "discovered_at_epoch_millis") val discoveredAtEpochMillis: Long,
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "source_modified_at_epoch_millis") val sourceModifiedAtEpochMillis: Long?,
    @ColumnInfo(name = "indexing_status") val indexingStatus: String,
    @ColumnInfo(name = "indexing_attempt_count") val indexingAttemptCount: Int,
    @ColumnInfo(name = "failure_code") val failureCode: String?,
    @ColumnInfo(name = "failure_message") val failureMessage: String?,
)

internal fun AssetIndexRecord.toEntity(): AssetEntity = AssetEntity(
    sourceId = asset.identity.sourceId.value,
    sourceAssetKey = asset.identity.sourceAssetKey.value,
    assetType = asset.type.name,
    location = asset.location.value,
    fingerprint = asset.fingerprint.value,
    discoveredAtEpochMillis = asset.discoveredAt.toEpochMilli(),
    displayName = asset.displayName,
    sourceModifiedAtEpochMillis = asset.sourceModifiedAt?.toEpochMilli(),
    indexingStatus = indexingState.status.name,
    indexingAttemptCount = indexingState.attemptCount,
    failureCode = indexingState.failure?.code,
    failureMessage = indexingState.failure?.message,
)

internal fun AssetEntity.toDomain(): AssetIndexRecord {
    val failure = when {
        failureCode == null && failureMessage == null -> null
        failureCode != null && failureMessage != null -> IndexingFailure(failureCode, failureMessage)
        else -> error("Persisted indexing failure details are incomplete for $sourceId:$sourceAssetKey.")
    }

    return AssetIndexRecord(
        asset = Asset(
            identity = AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
            type = AssetType.valueOf(assetType),
            location = AssetLocation(location),
            fingerprint = AssetFingerprint(fingerprint),
            discoveredAt = Instant.ofEpochMilli(discoveredAtEpochMillis),
            displayName = displayName,
            sourceModifiedAt = sourceModifiedAtEpochMillis?.let(Instant::ofEpochMilli),
        ),
        indexingState = IndexingState(
            status = IndexingStatus.valueOf(indexingStatus),
            attemptCount = indexingAttemptCount,
            failure = failure,
        ),
    )
}
