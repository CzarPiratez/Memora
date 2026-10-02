package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.indexing.IndexFailureClass
import com.memora.app.domain.indexing.IndexStage
import com.memora.app.domain.indexing.IndexStageState
import com.memora.app.domain.indexing.IndexStageStatus

@Entity(
    tableName = "index_stage_states",
    primaryKeys = ["source_id", "source_asset_key", "fingerprint", "stage", "derivation_id"],
    indices = [
        Index(value = ["source_id", "source_asset_key"]),
        Index(value = ["fingerprint"]),
        Index(value = ["current_status"]),
        Index(value = ["stage", "current_status"]),
    ],
)
data class IndexStageStateEntity(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "fingerprint") val fingerprint: String,
    @ColumnInfo(name = "stage") val stage: String,
    @ColumnInfo(name = "derivation_id") val derivationId: String,
    @ColumnInfo(name = "current_status") val currentStatus: String,
    @ColumnInfo(name = "last_attempt_status") val lastAttemptStatus: String,
    @ColumnInfo(name = "last_failure_class") val lastFailureClass: String?,
    @ColumnInfo(name = "last_failure_code") val lastFailureCode: String?,
    @ColumnInfo(name = "last_failure_message") val lastFailureMessage: String?,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int,
    @ColumnInfo(name = "run_id") val runId: String?,
    @ColumnInfo(name = "schema_version") val schemaVersion: String?,
    @ColumnInfo(name = "model_id") val modelId: String?,
    @ColumnInfo(name = "model_version") val modelVersion: String?,
    @ColumnInfo(name = "engine_version") val engineVersion: String?,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
)

internal fun IndexStageState.toEntity(): IndexStageStateEntity = IndexStageStateEntity(
    sourceId = assetIdentity.sourceId.value,
    sourceAssetKey = assetIdentity.sourceAssetKey.value,
    fingerprint = fingerprint.value,
    stage = stage.name,
    derivationId = derivationId,
    currentStatus = currentStatus.name,
    lastAttemptStatus = lastAttemptStatus,
    lastFailureClass = lastFailureClass?.name,
    lastFailureCode = lastFailureCode,
    lastFailureMessage = lastFailureMessage,
    attemptCount = attemptCount,
    runId = runId,
    schemaVersion = schemaVersion,
    modelId = modelId,
    modelVersion = modelVersion,
    engineVersion = engineVersion,
    updatedAtEpochMs = updatedAtEpochMs,
)

internal fun IndexStageStateEntity.toDomain(): IndexStageState = IndexStageState(
    assetIdentity = AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
    fingerprint = AssetFingerprint(fingerprint),
    stage = IndexStage.valueOf(stage),
    derivationId = derivationId,
    currentStatus = IndexStageStatus.valueOf(currentStatus),
    lastAttemptStatus = lastAttemptStatus,
    lastFailureClass = lastFailureClass?.let { IndexFailureClass.valueOf(it) },
    lastFailureCode = lastFailureCode,
    lastFailureMessage = lastFailureMessage,
    attemptCount = attemptCount,
    runId = runId,
    schemaVersion = schemaVersion,
    modelId = modelId,
    modelVersion = modelVersion,
    engineVersion = engineVersion,
    updatedAtEpochMs = updatedAtEpochMs,
)
