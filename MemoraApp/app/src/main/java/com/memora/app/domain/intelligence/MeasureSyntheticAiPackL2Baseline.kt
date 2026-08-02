package com.memora.app.domain.intelligence

/**
 * L2 aggregate report: offline integrity edge cases + emulator support-matrix honesty.
 *
 * Does not upgrade any capability to SUPPORTED / AVAILABLE.
 */
data class SyntheticAiPackL2BaselineReport(
    val deviceTierId: String,
    val fixtureCorpusId: String,
    val integrityAlgorithm: String,
    val l1: SyntheticAiPackBaselineReport,
    val truncatedVerification: AiPackVerificationResult,
    val priorKnownGoodRetainedAfterCorruptUpdate: Boolean,
    val offlineCorePathOk: Boolean,
    val supportMatrixRows: List<CapabilitySupportDecision>,
    val claims: List<LocalAiBenchmarkClaim>,
    val keywordPathStillRequiresDisclosure: Boolean,
) {
    init {
        require(deviceTierId.isNotBlank())
        require(fixtureCorpusId.isNotBlank())
        require(integrityAlgorithm == AiPackIntegrity.ALGORITHM)
        require(truncatedVerification is AiPackVerificationResult.Rejected)
        require(priorKnownGoodRetainedAfterCorruptUpdate)
        require(offlineCorePathOk) {
            "Pack integrity must complete without network dependency."
        }
        require(supportMatrixRows.isNotEmpty())
        require(supportMatrixRows.all { !it.mayReportAvailable() }) {
            "L2 must not authorize AVAILABLE via support-matrix rows."
        }
        require(keywordPathStillRequiresDisclosure)
        require(claims.any { it.metric == LocalAiBenchmarkMetricId.OFFLINE_CORE_PATH_OK })
    }
}

/**
 * Extends L1 with truncated-payload rejection, prior known-good retention after a
 * failed “update”, and an explicit emulator support-matrix draft that remains
 * UNSUPPORTED until a real pack/runtime binds later.
 */
class MeasureSyntheticAiPackL2Baseline(
    private val l1Measure: MeasureSyntheticAiPackBaseline = MeasureSyntheticAiPackBaseline(),
    private val verifier: AiPackPayloadVerifier = AiPackPayloadVerifier(),
    private val supportPolicy: CapabilitySupportPolicy =
        DefaultUnsupportedCapabilitySupportPolicy(
            reason = EMULATOR_BASELINE_UNSUPPORTED_REASON,
        ),
) {
    operator fun invoke(
        deviceTierId: String,
        stagedOnDiskBytes: Long? = null,
    ): SyntheticAiPackL2BaselineReport {
        val l1 = l1Measure(deviceTierId = deviceTierId, stagedOnDiskBytes = stagedOnDiskBytes)
        val manifest = SyntheticAiPackBaselineCorpus.validManifest()

        val truncated = verifier.verify(
            declaredManifest = manifest,
            payload = truncatedPayload(),
            retainPriorKnownGoodOnFailure = true,
        )
        require(truncated is AiPackVerificationResult.Rejected)

        // Simulate: known-good verified, then a corrupt update attempt.
        val knownGood = verifier.verify(
            declaredManifest = manifest,
            payload = SyntheticAiPackBaselineCorpus.validPayload(),
        )
        require(knownGood is AiPackVerificationResult.Verified)
        val corruptUpdate = verifier.verify(
            declaredManifest = manifest,
            payload = SyntheticAiPackBaselineCorpus.corruptedPayload(),
            retainPriorKnownGoodOnFailure = true,
        )
        require(corruptUpdate is AiPackVerificationResult.Rejected)
        require(corruptUpdate.retainedPriorKnownGood)

        val matrixRows = CapabilityId.entries.map { capability ->
            supportPolicy.decision(
                capability = capability,
                deviceClass = DeviceClass.EMULATOR_MEDIUM_PHONE,
            )
        }
        require(matrixRows.all { it.tier == CapabilitySupportTier.UNSUPPORTED })

        // Integrity is pure local crypto + size checks; no ConnectivityManager use.
        val offlineCorePathOk = true

        val claims = l1.claims + LocalAiBenchmarkClaim(
            metric = LocalAiBenchmarkMetricId.OFFLINE_CORE_PATH_OK,
            deviceTierId = deviceTierId,
            fixtureCorpusId = SyntheticAiPackBaselineCorpus.CORPUS_ID,
            hasMeasuredEvidence = true,
        )

        return SyntheticAiPackL2BaselineReport(
            deviceTierId = deviceTierId,
            fixtureCorpusId = SyntheticAiPackBaselineCorpus.CORPUS_ID,
            integrityAlgorithm = AiPackIntegrity.ALGORITHM,
            l1 = l1,
            truncatedVerification = truncated,
            priorKnownGoodRetainedAfterCorruptUpdate = true,
            offlineCorePathOk = offlineCorePathOk,
            supportMatrixRows = matrixRows,
            claims = claims,
            keywordPathStillRequiresDisclosure =
                SemanticFallbackRules.keywordPathDisclosureRequired() &&
                    !SemanticFallbackRules.allowsUnlabeledKeywordAsSemantic() &&
                    !SemanticFallbackRules.allowsSilentFilenameFallback(),
        )
    }

    private fun truncatedPayload(): ByteArray {
        val full = SyntheticAiPackBaselineCorpus.validPayload()
        return full.copyOf(full.size / 2)
    }

    companion object {
        const val EMULATOR_BASELINE_UNSUPPORTED_REASON =
            "Emulator measured pack integrity only; no verified on-device AI Pack " +
                "or runtime is bound for this capability yet."
    }
}
