package com.memora.app.domain.indexing

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity

/** Pipeline stages for Index Fabric state tracking. */
enum class IndexStage {
    DISCOVERY,
    EXIF,
    OCR,
    PDF_PARSE,
    ASSEMBLY,
    EMBEDDING,
}

/** Operational lifecycle validity state of a stage's derived output. */
enum class IndexStageStatus {
    DISCOVERED,
    QUEUED,
    RUNNING,
    VALID,
    STALE,
    FAILED,
}

/** Operational taxonomy for stage execution failures. */
enum class IndexFailureClass {
    RETRYABLE,
    PERMANENT,
    ACCESS_REVOKED,
    RESOURCE,
    MODEL_UNAVAILABLE,
}

/** Domain representation of an asset version's processing state for one stage. */
data class IndexStageState(
    val assetIdentity: AssetIdentity,
    val fingerprint: AssetFingerprint,
    val stage: IndexStage,
    val derivationId: String,
    val currentStatus: IndexStageStatus,
    val lastAttemptStatus: String,
    val lastFailureClass: IndexFailureClass? = null,
    val lastFailureCode: String? = null,
    val lastFailureMessage: String? = null,
    val attemptCount: Int = 1,
    val runId: String? = null,
    val schemaVersion: String? = null,
    val modelId: String? = null,
    val modelVersion: String? = null,
    val engineVersion: String? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
) {
    init {
        require(derivationId.isNotBlank()) { "A derivation ID cannot be blank." }
        require(attemptCount >= 0) { "Attempt count cannot be negative." }
        require(updatedAtEpochMs >= 0) { "Updated timestamp cannot be negative." }
    }

    companion object {
        fun computeDerivationId(
            stage: IndexStage,
            schemaVersion: String? = null,
            modelId: String? = null,
            modelVersion: String? = null,
            engineVersion: String? = null,
        ): String = listOfNotNull(
            stage.name.lowercase(),
            schemaVersion?.takeIf { it.isNotBlank() },
            modelId?.takeIf { it.isNotBlank() },
            modelVersion?.takeIf { it.isNotBlank() },
            engineVersion?.takeIf { it.isNotBlank() },
        ).joinToString(":")
    }
}
