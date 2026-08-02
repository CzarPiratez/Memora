package com.memora.app.domain.intelligence

import java.security.MessageDigest
import javax.inject.Inject

/**
 * Cryptographic integrity helpers for AI Pack payloads (Spec §6 / ADR-023).
 *
 * Algorithm is recorded so measured baselines never leave the hash scheme implicit.
 * This type performs no network I/O and never activates a product capability.
 */
object AiPackIntegrity {
    const val ALGORITHM = "SHA-256"

    fun sha256Hex(payload: ByteArray): String {
        val digest = MessageDigest.getInstance(ALGORITHM).digest(payload)
        return digest.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }
}

/**
 * Pure payload verification against a declared [AiPackManifest].
 *
 * Success yields [AiPackVerificationResult.Verified] with ACTIVE state for the
 * harness only. Product UI and engine availability must not treat this as an
 * AVAILABLE Local Intelligence claim without a later change-controlled slice.
 */
class AiPackPayloadVerifier @Inject constructor() {
    fun verify(
        declaredManifest: AiPackManifest,
        payload: ByteArray,
        retainPriorKnownGoodOnFailure: Boolean = false,
    ): AiPackVerificationResult {
        if (payload.isEmpty()) {
            return AiPackVerificationResult.Rejected(
                reason = "Pack payload is empty.",
                retainedPriorKnownGood = retainPriorKnownGoodOnFailure,
            )
        }
        if (payload.size.toLong() != declaredManifest.downloadSizeBytes) {
            return AiPackVerificationResult.Rejected(
                reason = "Pack payload size does not match the disclosed download size.",
                retainedPriorKnownGood = retainPriorKnownGoodOnFailure,
            )
        }
        val actualHash = AiPackIntegrity.sha256Hex(payload)
        if (!actualHash.equals(declaredManifest.integrityHash, ignoreCase = true)) {
            return AiPackVerificationResult.Rejected(
                reason = "Pack integrity hash mismatch (${AiPackIntegrity.ALGORITHM}).",
                retainedPriorKnownGood = retainPriorKnownGoodOnFailure,
            )
        }
        return AiPackVerificationResult.Verified(
            declaredManifest.copy(installationState = AiPackInstallState.ACTIVE),
        )
    }
}
