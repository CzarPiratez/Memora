package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.AiPackInstallLedger
import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.AiPackPayloadStore
import com.memora.app.domain.intelligence.AiPackPayloadVerifier
import com.memora.app.domain.intelligence.AiPackVerificationResult
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.EmbeddingFirstOfflinePackFixture
import javax.inject.Inject

/**
 * E4a: after disclosure, stage the offline embedding pack container, verify
 * integrity, and mark the ledger ACTIVE — without network and without binding
 * [com.memora.app.domain.intelligence.EmbeddingEngine].
 */
class ActivateOfflineEmbeddingPackContainer @Inject constructor(
    private val ledger: AiPackInstallLedger,
    private val payloadStore: AiPackPayloadStore,
    private val verifier: AiPackPayloadVerifier,
) {
    operator fun invoke(nowEpochMs: Long): ActivateOfflineEmbeddingPackResult {
        require(nowEpochMs >= 0)
        val packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID
        val prior = ledger.entry(packId)
            ?: return ActivateOfflineEmbeddingPackResult.DisclosureRequired
        if (prior.disclosureAcknowledgedAtEpochMs == null) {
            return ActivateOfflineEmbeddingPackResult.DisclosureRequired
        }
        if (prior.installationState == AiPackInstallState.ACTIVE) {
            return ActivateOfflineEmbeddingPackResult.AlreadyActive
        }

        ledger.beginVerification(packId, atEpochMs = nowEpochMs)
        val payload = EmbeddingFirstOfflinePackFixture.validPayload()
        val manifest = EmbeddingFirstOfflinePackFixture.validManifest()

        return try {
            payloadStore.writePayload(packId, payload)
            when (
                val outcome = verifier.verify(
                    declaredManifest = manifest,
                    payload = payload,
                    retainPriorKnownGoodOnFailure = false,
                )
            ) {
                is AiPackVerificationResult.Verified -> {
                    ledger.recordVerifiedActive(outcome.manifest, atEpochMs = nowEpochMs + 1)
                    ActivateOfflineEmbeddingPackResult.Activated
                }
                is AiPackVerificationResult.Rejected -> {
                    payloadStore.deletePayload(packId)
                    ledger.recordVerificationFailed(
                        packId = packId,
                        reason = outcome.reason,
                        retainPriorKnownGood = false,
                        atEpochMs = nowEpochMs + 1,
                    )
                    ActivateOfflineEmbeddingPackResult.Failed(outcome.reason)
                }
            }
        } catch (error: Exception) {
            payloadStore.deletePayload(packId)
            val reason = error.message?.takeIf { it.isNotBlank() }
                ?: "Pack container could not be stored."
            ledger.recordVerificationFailed(
                packId = packId,
                reason = reason,
                retainPriorKnownGood = false,
                atEpochMs = nowEpochMs + 1,
            )
            ActivateOfflineEmbeddingPackResult.Failed(reason)
        }
    }
}

sealed interface ActivateOfflineEmbeddingPackResult {
    data object DisclosureRequired : ActivateOfflineEmbeddingPackResult

    data object AlreadyActive : ActivateOfflineEmbeddingPackResult

    data object Activated : ActivateOfflineEmbeddingPackResult

    data class Failed(val reason: String) : ActivateOfflineEmbeddingPackResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
