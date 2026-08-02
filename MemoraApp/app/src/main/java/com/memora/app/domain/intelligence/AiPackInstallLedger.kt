package com.memora.app.domain.intelligence

/**
 * User-visible disclosure fields that must be acknowledged before install work
 * (Local AI Spec §6 / ADR-023). Holds no model bytes and no user content.
 */
data class AiPackDisclosureSnapshot(
    val packId: String,
    val capability: CapabilityId,
    val model: ModelVersionIdentity,
    val downloadSizeBytes: Long,
    val storageRequirementBytes: Long,
    val license: String,
    val disclosedAtEpochMs: Long,
) {
    init {
        require(packId.isNotBlank()) { "Disclosure needs a non-blank pack id." }
        require(downloadSizeBytes > 0) { "Disclosure download size must be positive." }
        require(storageRequirementBytes > 0) {
            "Disclosure storage requirement must be positive."
        }
        require(license.isNotBlank()) { "Disclosure needs a non-blank license." }
        require(disclosedAtEpochMs >= 0) { "Disclosure time must not be negative." }
    }
}

/**
 * Durable-facing install record for one pack id.
 *
 * [ACTIVE] is allowed only when [verifiedIntegrityHash] is present and matches
 * the verified manifest path — never invented from blank data.
 */
data class AiPackInstallLedgerEntry(
    val packId: String,
    val capability: CapabilityId,
    val installationState: AiPackInstallState,
    val model: ModelVersionIdentity?,
    val compatibleAppVersions: String?,
    val compatibleSchemaVersions: String?,
    val downloadSizeBytes: Long?,
    val storageRequirementBytes: Long?,
    val license: String?,
    val verifiedIntegrityHash: String?,
    val disclosureAcknowledgedAtEpochMs: Long?,
    val failureReason: String?,
    val updatedAtEpochMs: Long,
) {
    init {
        require(packId.isNotBlank()) { "Ledger entry needs a non-blank pack id." }
        require(updatedAtEpochMs >= 0)
        if (installationState == AiPackInstallState.ACTIVE) {
            require(!verifiedIntegrityHash.isNullOrBlank()) {
                "ACTIVE packs must retain a verified integrity hash."
            }
            require(model != null) { "ACTIVE packs must retain model identity." }
            require(!compatibleAppVersions.isNullOrBlank())
            require(!compatibleSchemaVersions.isNullOrBlank())
            require(downloadSizeBytes != null && downloadSizeBytes > 0)
            require(storageRequirementBytes != null && storageRequirementBytes > 0)
            require(!license.isNullOrBlank())
            require(disclosureAcknowledgedAtEpochMs != null) {
                "ACTIVE packs require prior disclosure acknowledgment."
            }
        }
        if (installationState == AiPackInstallState.FAILED_VERIFICATION) {
            require(!failureReason.isNullOrBlank()) {
                "Failed verification must explain why."
            }
        }
    }

    fun toActiveManifestOrNull(): AiPackManifest? {
        if (installationState != AiPackInstallState.ACTIVE) return null
        return AiPackManifest(
            packId = packId,
            capability = capability,
            model = checkNotNull(model),
            compatibleAppVersions = checkNotNull(compatibleAppVersions),
            compatibleSchemaVersions = checkNotNull(compatibleSchemaVersions),
            downloadSizeBytes = checkNotNull(downloadSizeBytes),
            storageRequirementBytes = checkNotNull(storageRequirementBytes),
            integrityHash = checkNotNull(verifiedIntegrityHash),
            license = checkNotNull(license),
            installationState = AiPackInstallState.ACTIVE,
        )
    }
}

/**
 * Domain install ledger: disclosure → verify → activate / fail / retain known-good.
 *
 * Implementations may be in-memory (tests) or Room-backed. No download or inference.
 */
interface AiPackInstallLedger {
    fun entry(packId: String): AiPackInstallLedgerEntry?

    fun acknowledgeDisclosure(disclosure: AiPackDisclosureSnapshot): AiPackInstallLedgerEntry

    fun beginVerification(packId: String, atEpochMs: Long): AiPackInstallLedgerEntry

    fun recordVerifiedActive(
        manifest: AiPackManifest,
        atEpochMs: Long,
    ): AiPackInstallLedgerEntry

    fun recordVerificationFailed(
        packId: String,
        reason: String,
        retainPriorKnownGood: Boolean,
        atEpochMs: Long,
    ): AiPackInstallLedgerEntry
}

/**
 * First implementation track identity (ADR-029). Vendor/model bytes remain TBD.
 */
object EmbeddingFirstAiPackTrack {
    val capability: CapabilityId = CapabilityId.EMBEDDING

    /** Stable id namespace for the future embedding pack — not a downloadable artifact. */
    const val PLANNED_PACK_ID = "memora-embedding-pack-v1"
}

/**
 * Pure state transitions shared by in-memory and Room ledgers.
 */
object AiPackInstallTransitions {
    fun acknowledgeDisclosure(
        prior: AiPackInstallLedgerEntry?,
        disclosure: AiPackDisclosureSnapshot,
    ): AiPackInstallLedgerEntry = AiPackInstallLedgerEntry(
        packId = disclosure.packId,
        capability = disclosure.capability,
        installationState = when (prior?.installationState) {
            AiPackInstallState.ACTIVE -> AiPackInstallState.ACTIVE
            AiPackInstallState.VERIFYING -> AiPackInstallState.VERIFYING
            else -> AiPackInstallState.NOT_INSTALLED
        },
        model = disclosure.model,
        compatibleAppVersions = prior?.compatibleAppVersions,
        compatibleSchemaVersions = prior?.compatibleSchemaVersions,
        downloadSizeBytes = disclosure.downloadSizeBytes,
        storageRequirementBytes = disclosure.storageRequirementBytes,
        license = disclosure.license,
        verifiedIntegrityHash = prior?.verifiedIntegrityHash,
        disclosureAcknowledgedAtEpochMs = disclosure.disclosedAtEpochMs,
        failureReason = null,
        updatedAtEpochMs = disclosure.disclosedAtEpochMs,
    )

    fun beginVerification(
        prior: AiPackInstallLedgerEntry?,
        packId: String,
        atEpochMs: Long,
    ): AiPackInstallLedgerEntry {
        require(packId.isNotBlank())
        require(atEpochMs >= 0)
        require(prior != null) {
            "Disclosure must be acknowledged before verification begins."
        }
        require(prior.disclosureAcknowledgedAtEpochMs != null) {
            "Disclosure must be acknowledged before verification begins."
        }
        return prior.copy(
            installationState = AiPackInstallState.VERIFYING,
            failureReason = null,
            updatedAtEpochMs = atEpochMs,
        )
    }

    fun recordVerifiedActive(
        prior: AiPackInstallLedgerEntry?,
        manifest: AiPackManifest,
        atEpochMs: Long,
    ): AiPackInstallLedgerEntry {
        require(atEpochMs >= 0)
        require(manifest.installationState == AiPackInstallState.ACTIVE) {
            "Only ACTIVE manifests may be recorded as verified."
        }
        require(prior != null) { "Disclosure must be acknowledged before activation." }
        require(prior.disclosureAcknowledgedAtEpochMs != null) {
            "Disclosure must be acknowledged before activation."
        }
        require(prior.installationState == AiPackInstallState.VERIFYING) {
            "Activation requires an in-progress verification."
        }
        return AiPackInstallLedgerEntry(
            packId = manifest.packId,
            capability = manifest.capability,
            installationState = AiPackInstallState.ACTIVE,
            model = manifest.model,
            compatibleAppVersions = manifest.compatibleAppVersions,
            compatibleSchemaVersions = manifest.compatibleSchemaVersions,
            downloadSizeBytes = manifest.downloadSizeBytes,
            storageRequirementBytes = manifest.storageRequirementBytes,
            license = manifest.license,
            verifiedIntegrityHash = manifest.integrityHash,
            disclosureAcknowledgedAtEpochMs = prior.disclosureAcknowledgedAtEpochMs,
            failureReason = null,
            updatedAtEpochMs = atEpochMs,
        )
    }

    fun recordVerificationFailed(
        prior: AiPackInstallLedgerEntry?,
        packId: String,
        reason: String,
        retainPriorKnownGood: Boolean,
        atEpochMs: Long,
    ): AiPackInstallLedgerEntry {
        require(packId.isNotBlank())
        require(reason.isNotBlank()) { "Failure reason must not be blank." }
        require(atEpochMs >= 0)
        require(prior != null) {
            "Disclosure must be acknowledged before verification failure."
        }
        require(prior.disclosureAcknowledgedAtEpochMs != null)

        return if (
            retainPriorKnownGood &&
            prior.verifiedIntegrityHash != null &&
            prior.model != null &&
            !prior.compatibleAppVersions.isNullOrBlank() &&
            !prior.compatibleSchemaVersions.isNullOrBlank() &&
            prior.downloadSizeBytes != null &&
            prior.storageRequirementBytes != null &&
            !prior.license.isNullOrBlank()
        ) {
            prior.copy(
                installationState = AiPackInstallState.ACTIVE,
                failureReason = null,
                updatedAtEpochMs = atEpochMs,
            )
        } else {
            prior.copy(
                installationState = AiPackInstallState.FAILED_VERIFICATION,
                failureReason = reason,
                verifiedIntegrityHash = if (retainPriorKnownGood) {
                    prior.verifiedIntegrityHash
                } else {
                    null
                },
                updatedAtEpochMs = atEpochMs,
            )
        }
    }
}

/** In-memory ledger for domain unit tests. */
class InMemoryAiPackInstallLedger : AiPackInstallLedger {
    private val entries = linkedMapOf<String, AiPackInstallLedgerEntry>()

    override fun entry(packId: String): AiPackInstallLedgerEntry? {
        require(packId.isNotBlank())
        return entries[packId]
    }

    override fun acknowledgeDisclosure(
        disclosure: AiPackDisclosureSnapshot,
    ): AiPackInstallLedgerEntry {
        val next = AiPackInstallTransitions.acknowledgeDisclosure(
            prior = entries[disclosure.packId],
            disclosure = disclosure,
        )
        entries[disclosure.packId] = next
        return next
    }

    override fun beginVerification(packId: String, atEpochMs: Long): AiPackInstallLedgerEntry {
        val next = AiPackInstallTransitions.beginVerification(
            prior = entries[packId],
            packId = packId,
            atEpochMs = atEpochMs,
        )
        entries[packId] = next
        return next
    }

    override fun recordVerifiedActive(
        manifest: AiPackManifest,
        atEpochMs: Long,
    ): AiPackInstallLedgerEntry {
        val next = AiPackInstallTransitions.recordVerifiedActive(
            prior = entries[manifest.packId],
            manifest = manifest,
            atEpochMs = atEpochMs,
        )
        entries[manifest.packId] = next
        return next
    }

    override fun recordVerificationFailed(
        packId: String,
        reason: String,
        retainPriorKnownGood: Boolean,
        atEpochMs: Long,
    ): AiPackInstallLedgerEntry {
        val next = AiPackInstallTransitions.recordVerificationFailed(
            prior = entries[packId],
            packId = packId,
            reason = reason,
            retainPriorKnownGood = retainPriorKnownGood,
            atEpochMs = atEpochMs,
        )
        entries[packId] = next
        return next
    }
}

/**
 * [AiPackManager] backed by an install ledger — never invents ACTIVE from blank.
 *
 * Empty Room ledger reports NOT_INSTALLED / Rejected (same honesty as Unavailable).
 */
class LedgerBackedAiPackManager(
    private val ledger: AiPackInstallLedger,
    private val unavailableReason: String = DEFAULT_UNAVAILABLE_REASON,
) : AiPackManager {
    override fun installationState(packId: String): AiPackInstallState {
        require(packId.isNotBlank())
        return ledger.entry(packId)?.installationState ?: AiPackInstallState.NOT_INSTALLED
    }

    override fun verifiedManifest(packId: String): AiPackVerificationResult {
        require(packId.isNotBlank())
        val entry = ledger.entry(packId)
        val active = entry?.toActiveManifestOrNull()
        if (active != null) {
            return AiPackVerificationResult.Verified(active)
        }
        val reason = when {
            entry == null -> unavailableReason
            entry.installationState == AiPackInstallState.FAILED_VERIFICATION ->
                entry.failureReason ?: unavailableReason
            entry.installationState == AiPackInstallState.VERIFYING ->
                "AI Pack verification is still in progress."
            else -> unavailableReason
        }
        return AiPackVerificationResult.Rejected(
            reason = reason,
            retainedPriorKnownGood = false,
        )
    }

    private companion object {
        const val DEFAULT_UNAVAILABLE_REASON =
            "No on-device AI Pack is installed on this phone yet."
    }
}
