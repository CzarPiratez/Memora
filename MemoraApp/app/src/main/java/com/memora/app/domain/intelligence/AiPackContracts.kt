package com.memora.app.domain.intelligence

/**
 * Installation lifecycle for a local AI Pack (Local AI Spec §6).
 *
 * Domain code never invents [ACTIVE] without a verified manifest.
 */
enum class AiPackInstallState {
    NOT_INSTALLED,
    VERIFYING,
    ACTIVE,
    FAILED_VERIFICATION,
    ROLLED_BACK,
}

/**
 * Approved pack manifest fields required before any future download/inference work.
 *
 * This type is availability/metadata only. It performs no I/O and holds no model bytes.
 */
data class AiPackManifest(
    val packId: String,
    val capability: CapabilityId,
    val model: ModelVersionIdentity,
    val compatibleAppVersions: String,
    val compatibleSchemaVersions: String,
    val downloadSizeBytes: Long,
    val storageRequirementBytes: Long,
    val integrityHash: String,
    val license: String,
    val installationState: AiPackInstallState,
) {
    init {
        require(packId.isNotBlank()) { "An AI Pack needs a non-blank pack id." }
        require(compatibleAppVersions.isNotBlank()) {
            "An AI Pack needs compatible application versions."
        }
        require(compatibleSchemaVersions.isNotBlank()) {
            "An AI Pack needs compatible schema versions."
        }
        require(downloadSizeBytes > 0) { "Download size must be a positive byte count." }
        require(storageRequirementBytes > 0) {
            "Storage requirement must be a positive byte count."
        }
        require(integrityHash.isNotBlank()) {
            "An AI Pack needs a non-blank integrity hash."
        }
        require(license.isNotBlank()) { "An AI Pack needs a non-blank license." }
    }
}

/**
 * Outcome of atomic pack verification (Spec §6).
 *
 * Failed or incomplete verification must not activate a pack.
 */
sealed interface AiPackVerificationResult {
    data class Verified(
        val manifest: AiPackManifest,
    ) : AiPackVerificationResult {
        init {
            require(manifest.installationState == AiPackInstallState.ACTIVE) {
                "A verified pack must report ACTIVE installation state."
            }
        }
    }

    data class Rejected(
        val reason: String,
        val retainedPriorKnownGood: Boolean,
    ) : AiPackVerificationResult {
        init {
            require(reason.isNotBlank()) {
                "A rejected pack verification must explain why."
            }
        }
    }
}

/**
 * Domain surface for pack registry checks.
 *
 * Implementations live in platform/data adapters. This slice provides only the
 * contract and an unavailable stub — no download, network, or model bytes.
 */
interface AiPackManager {
    fun installationState(packId: String): AiPackInstallState

    fun verifiedManifest(packId: String): AiPackVerificationResult
}

/**
 * Truthful stub: no pack is installed until a later approved delivery slice.
 */
class UnavailableAiPackManager(
    private val reason: String = DEFAULT_UNAVAILABLE_REASON,
) : AiPackManager {
    override fun installationState(packId: String): AiPackInstallState {
        require(packId.isNotBlank()) { "Pack id must not be blank." }
        return AiPackInstallState.NOT_INSTALLED
    }

    override fun verifiedManifest(packId: String): AiPackVerificationResult {
        require(packId.isNotBlank()) { "Pack id must not be blank." }
        return AiPackVerificationResult.Rejected(
            reason = reason,
            retainedPriorKnownGood = false,
        )
    }
}

private const val DEFAULT_UNAVAILABLE_REASON =
    "No on-device AI Pack is installed on this phone yet."
