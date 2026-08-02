package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.memora.app.domain.intelligence.AiPackInstallLedgerEntry
import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.CapabilityId
import com.memora.app.domain.intelligence.ModelVersionIdentity

/**
 * Room row for one AI Pack install-ledger entry (ADR-029 E2).
 *
 * Holds Memora-owned install metadata only — never model bytes or user content.
 */
@Entity(tableName = "ai_pack_install_ledger")
data class AiPackInstallLedgerEntity(
    @PrimaryKey
    @ColumnInfo(name = "pack_id") val packId: String,
    @ColumnInfo(name = "capability") val capability: String,
    @ColumnInfo(name = "installation_state") val installationState: String,
    @ColumnInfo(name = "model_id") val modelId: String?,
    @ColumnInfo(name = "model_version") val modelVersion: String?,
    @ColumnInfo(name = "compatible_app_versions") val compatibleAppVersions: String?,
    @ColumnInfo(name = "compatible_schema_versions") val compatibleSchemaVersions: String?,
    @ColumnInfo(name = "download_size_bytes") val downloadSizeBytes: Long?,
    @ColumnInfo(name = "storage_requirement_bytes") val storageRequirementBytes: Long?,
    @ColumnInfo(name = "license") val license: String?,
    @ColumnInfo(name = "verified_integrity_hash") val verifiedIntegrityHash: String?,
    @ColumnInfo(name = "disclosure_acknowledged_at_epoch_ms")
    val disclosureAcknowledgedAtEpochMs: Long?,
    @ColumnInfo(name = "failure_reason") val failureReason: String?,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
)

internal fun AiPackInstallLedgerEntry.toEntity(): AiPackInstallLedgerEntity =
    AiPackInstallLedgerEntity(
        packId = packId,
        capability = capability.name,
        installationState = installationState.name,
        modelId = model?.modelId,
        modelVersion = model?.version,
        compatibleAppVersions = compatibleAppVersions,
        compatibleSchemaVersions = compatibleSchemaVersions,
        downloadSizeBytes = downloadSizeBytes,
        storageRequirementBytes = storageRequirementBytes,
        license = license,
        verifiedIntegrityHash = verifiedIntegrityHash,
        disclosureAcknowledgedAtEpochMs = disclosureAcknowledgedAtEpochMs,
        failureReason = failureReason,
        updatedAtEpochMs = updatedAtEpochMs,
    )

internal fun AiPackInstallLedgerEntity.toDomain(): AiPackInstallLedgerEntry {
    val model = when {
        modelId.isNullOrBlank() || modelVersion.isNullOrBlank() -> null
        else -> ModelVersionIdentity(modelId = modelId, version = modelVersion)
    }
    return AiPackInstallLedgerEntry(
        packId = packId,
        capability = CapabilityId.valueOf(capability),
        installationState = AiPackInstallState.valueOf(installationState),
        model = model,
        compatibleAppVersions = compatibleAppVersions,
        compatibleSchemaVersions = compatibleSchemaVersions,
        downloadSizeBytes = downloadSizeBytes,
        storageRequirementBytes = storageRequirementBytes,
        license = license,
        verifiedIntegrityHash = verifiedIntegrityHash,
        disclosureAcknowledgedAtEpochMs = disclosureAcknowledgedAtEpochMs,
        failureReason = failureReason,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}
