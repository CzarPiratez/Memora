package com.memora.app.domain.intelligence

/**
 * Scores labeled page-recall cases with a live [EmbeddingEngine] (M2).
 *
 * Does not authorize product AVAILABLE. Aggregate metrics only — callers must
 * not log cue/evidence text in production logs.
 */
object ScoreMeaningPdfPageRecallCorpus {
    fun score(
        engine: EmbeddingEngine,
        labeledCases: List<MeaningPdfPageRecallLabeledCase> =
            MeaningPdfPageRecallCorpus.labeledCases(),
    ): ScoreMeaningPdfPageRecallResult {
        require(labeledCases.isNotEmpty())
        val startedAt = System.nanoTime()
        var embedCount = 0
        val scored = labeledCases.map { labeled ->
            val query = embedOrThrow(engine, labeled.cue)
            embedCount += 1
            val candidates = labeled.candidates.map { candidate ->
                val vector = embedOrThrow(engine, candidate.evidenceText)
                embedCount += 1
                MeaningPdfPageRecallCandidate(
                    assetFileName = candidate.assetFileName,
                    pageNumber = candidate.pageNumber,
                    evidenceText = candidate.evidenceText,
                    injectedCosine = EmbeddingSimilarity.cosine(query, vector),
                )
            }
            MeaningPdfPageRecallCase(
                caseId = labeled.caseId,
                cue = labeled.cue,
                expectedAssetFileName = labeled.expectedAssetFileName,
                expectedPageNumber = labeled.expectedPageNumber,
                candidates = candidates,
            )
        }
        val wallMs = ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L)
        val model = when (val availability = engine.availability()) {
            is CapabilityAvailability.Available -> availability.model
            else -> MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY
        }
        return ScoreMeaningPdfPageRecallResult(
            scoredCases = scored,
            model = model,
            embedCount = embedCount,
            embedWallMs = wallMs,
        )
    }

    private fun embedOrThrow(engine: EmbeddingEngine, text: String): EmbeddingVector =
        when (val result = engine.embedText(text)) {
            is EmbeddingEncodeResult.Success -> result.vector
            is EmbeddingEncodeResult.Unavailable ->
                error("Embedding unavailable: ${result.reason}")
            is EmbeddingEncodeResult.Failed ->
                error("Embedding failed: ${result.reason}")
        }
}

data class ScoreMeaningPdfPageRecallResult(
    val scoredCases: List<MeaningPdfPageRecallCase>,
    val model: ModelVersionIdentity,
    val embedCount: Int,
    val embedWallMs: Long,
) {
    init {
        require(scoredCases.isNotEmpty())
        require(embedCount > 0)
        require(embedWallMs >= 0L)
    }
}

/**
 * M2 aggregate wrapper: live-scored cases + baseline report without enforcing
 * the M1 injected-boost bar (live compact quality is the measurement).
 */
object MeasureOnDeviceMeaningPdfPageRecallBaseline {
    fun measure(
        scoreResult: ScoreMeaningPdfPageRecallResult,
        deviceTierId: String = MeaningPdfPageRecallCorpus.DEVICE_TIER_EMULATOR_MEDIUM_PHONE,
    ): OnDeviceMeaningPdfPageRecallReport {
        val baseline = MeasureMeaningPdfPageRecallBaseline.measure(
            deviceTierId = deviceTierId,
            cases = scoreResult.scoredCases,
            notesPrefix = "M2 on-device harness (${scoreResult.model.modelId}@${scoreResult.model.version}). ",
            enforceBoostedHitBar = false,
            appendM2FollowUp = false,
        )
        return OnDeviceMeaningPdfPageRecallReport(
            baseline = baseline,
            model = scoreResult.model,
            embedCount = scoreResult.embedCount,
            embedWallMs = scoreResult.embedWallMs,
        )
    }
}

data class OnDeviceMeaningPdfPageRecallReport(
    val baseline: MeaningPdfPageRecallBaselineReport,
    val model: ModelVersionIdentity,
    val embedCount: Int,
    val embedWallMs: Long,
) {
    init {
        require(embedCount > 0)
        require(embedWallMs >= 0L)
    }
}
