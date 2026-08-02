package com.memora.app.domain.intelligence

/**
 * Privacy-safe aggregate report from one synthetic AI Pack baseline run.
 *
 * Contains no source text, URIs, or user content. Does not authorize product
 * AVAILABLE claims; [LocalAiBenchmarkClaim.hasMeasuredEvidence] only means this
 * harness observed the metric on [deviceTierId] for [fixtureCorpusId].
 */
data class SyntheticAiPackBaselineReport(
    val deviceTierId: String,
    val fixtureCorpusId: String,
    val integrityAlgorithm: String,
    val packOnDiskBytes: Long,
    val disclosedDownloadBytes: Long,
    val disclosedStorageRequirementBytes: Long,
    val validVerification: AiPackVerificationResult,
    val corruptedVerification: AiPackVerificationResult,
    val claims: List<LocalAiBenchmarkClaim>,
    val visionStillUnavailable: Boolean,
) {
    init {
        require(deviceTierId.isNotBlank())
        require(fixtureCorpusId.isNotBlank())
        require(integrityAlgorithm.isNotBlank())
        require(packOnDiskBytes > 0)
        require(disclosedDownloadBytes > 0)
        require(disclosedStorageRequirementBytes > 0)
        require(claims.isNotEmpty())
        require(visionStillUnavailable) {
            "Synthetic baseline must leave Vision unavailable for product claims."
        }
    }
}

/**
 * Runs the L1 synthetic pack size + integrity success/failure measurements.
 *
 * Never binds into product [AiPackManager] DI. Callers must tear down any staged
 * on-disk copies after android instrumentation.
 */
class MeasureSyntheticAiPackBaseline(
    private val verifier: AiPackPayloadVerifier = AiPackPayloadVerifier(),
    private val visionProbe: () -> CapabilityAvailability = {
        UnavailableLocalIntelligence.vision().availability()
    },
) {
    operator fun invoke(
        deviceTierId: String,
        stagedOnDiskBytes: Long? = null,
    ): SyntheticAiPackBaselineReport {
        require(deviceTierId.isNotBlank()) { "Device tier id is required." }

        val payload = SyntheticAiPackBaselineCorpus.validPayload()
        val manifest = SyntheticAiPackBaselineCorpus.validManifest()
        val onDisk = stagedOnDiskBytes ?: payload.size.toLong()

        val valid = verifier.verify(
            declaredManifest = manifest,
            payload = payload,
            retainPriorKnownGoodOnFailure = false,
        )
        val corrupted = verifier.verify(
            declaredManifest = manifest,
            payload = SyntheticAiPackBaselineCorpus.corruptedPayload(),
            retainPriorKnownGoodOnFailure = true,
        )

        require(valid is AiPackVerificationResult.Verified) {
            "Valid synthetic pack must verify."
        }
        require(corrupted is AiPackVerificationResult.Rejected) {
            "Corrupted synthetic pack must be rejected."
        }
        require((corrupted as AiPackVerificationResult.Rejected).retainedPriorKnownGood) {
            "Integrity failure must retain prior known-good when requested."
        }

        val visionAvailability = visionProbe()
        require(visionAvailability is CapabilityAvailability.Unavailable) {
            "Vision must remain Unavailable after the synthetic baseline."
        }

        val claims = listOf(
            LocalAiBenchmarkClaim(
                metric = LocalAiBenchmarkMetricId.PACK_ON_DISK_BYTES,
                deviceTierId = deviceTierId,
                fixtureCorpusId = SyntheticAiPackBaselineCorpus.CORPUS_ID,
                hasMeasuredEvidence = true,
            ),
            LocalAiBenchmarkClaim(
                metric = LocalAiBenchmarkMetricId.PACK_DOWNLOAD_BYTES,
                deviceTierId = deviceTierId,
                fixtureCorpusId = SyntheticAiPackBaselineCorpus.CORPUS_ID,
                hasMeasuredEvidence = true,
            ),
            LocalAiBenchmarkClaim(
                metric = LocalAiBenchmarkMetricId.PACK_INTEGRITY_FAILURE_HANDLED,
                deviceTierId = deviceTierId,
                fixtureCorpusId = SyntheticAiPackBaselineCorpus.CORPUS_ID,
                hasMeasuredEvidence = true,
            ),
        )

        return SyntheticAiPackBaselineReport(
            deviceTierId = deviceTierId,
            fixtureCorpusId = SyntheticAiPackBaselineCorpus.CORPUS_ID,
            integrityAlgorithm = AiPackIntegrity.ALGORITHM,
            packOnDiskBytes = onDisk,
            disclosedDownloadBytes = manifest.downloadSizeBytes,
            disclosedStorageRequirementBytes = manifest.storageRequirementBytes,
            validVerification = valid,
            corruptedVerification = corrupted,
            claims = claims,
            visionStillUnavailable = true,
        )
    }
}
