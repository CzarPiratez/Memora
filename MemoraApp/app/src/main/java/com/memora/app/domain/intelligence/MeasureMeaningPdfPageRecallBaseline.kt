package com.memora.app.domain.intelligence

/**
 * Page-hit @1 for cosine-only vs evidence-token-boosted ranking on a labeled
 * PDF page-recall corpus (M1 injected scores or M2 live-scored cases).
 *
 * Privacy-safe: no URIs or user content. Does not authorize product AVAILABLE UI.
 */
data class MeaningPdfPageRecallBaselineReport(
    val deviceTierId: String,
    val fixtureCorpusId: String,
    val caseCount: Int,
    val cosineOnlyHitsAt1: Int,
    val boostedHitsAt1: Int,
    val claims: List<LocalAiBenchmarkClaim>,
    val recommendsE4bForSemanticOnly: Boolean,
    val notes: String,
) {
    init {
        require(deviceTierId.isNotBlank())
        require(fixtureCorpusId.isNotBlank())
        require(caseCount > 0)
        require(cosineOnlyHitsAt1 in 0..caseCount)
        require(boostedHitsAt1 in 0..caseCount)
        require(claims.isNotEmpty())
        require(notes.isNotBlank())
    }

    val cosineOnlyHitRateAt1: Float get() = cosineOnlyHitsAt1.toFloat() / caseCount
    val boostedHitRateAt1: Float get() = boostedHitsAt1.toFloat() / caseCount
}

enum class MeaningPdfPageRecallRankingMode {
    COSINE_ONLY,
    EVIDENCE_TOKEN_BOOSTED,
}

object MeasureMeaningPdfPageRecallBaseline {
    /** Interim bar: M1 injected boosted path must hit all labeled cases @1. */
    const val BOOSTED_HIT_RATE_TARGET = 1.0f

    /**
     * If cosine-only hit rate is below this, larger embedder (E4b) is recommended
     * for semantic-only quality without token assist.
     */
    const val COSINE_ONLY_HIT_RATE_E4B_THRESHOLD = 0.67f

    fun measure(
        deviceTierId: String = MeaningPdfPageRecallCorpus.DEVICE_TIER_JVM_UNIT,
        cases: List<MeaningPdfPageRecallCase> = MeaningPdfPageRecallCorpus.cases(),
        notesPrefix: String = "M1 JVM harness on ${MeaningPdfPageRecallCorpus.CORPUS_ID}. ",
        enforceBoostedHitBar: Boolean = true,
        appendM2FollowUp: Boolean = true,
    ): MeaningPdfPageRecallBaselineReport {
        require(deviceTierId.isNotBlank())
        require(cases.isNotEmpty())
        require(notesPrefix.isNotBlank())

        val cosineHits = cases.count { case ->
            topMatchesExpected(case, MeaningPdfPageRecallRankingMode.COSINE_ONLY)
        }
        val boostedHits = cases.count { case ->
            topMatchesExpected(case, MeaningPdfPageRecallRankingMode.EVIDENCE_TOKEN_BOOSTED)
        }
        val cosineRate = cosineHits.toFloat() / cases.size
        val boostedRate = boostedHits.toFloat() / cases.size
        val recommendsE4b = cosineRate < COSINE_ONLY_HIT_RATE_E4B_THRESHOLD

        val claims = listOf(
            LocalAiBenchmarkClaim(
                metric = LocalAiBenchmarkMetricId.RECALL_AT_K,
                deviceTierId = deviceTierId,
                fixtureCorpusId = MeaningPdfPageRecallCorpus.CORPUS_ID,
                hasMeasuredEvidence = true,
            ),
            LocalAiBenchmarkClaim(
                metric = LocalAiBenchmarkMetricId.EXPLANATION_COVERAGE,
                deviceTierId = deviceTierId,
                fixtureCorpusId = MeaningPdfPageRecallCorpus.CORPUS_ID,
                hasMeasuredEvidence = true,
            ),
        )

        val notes = buildString {
            append(notesPrefix)
            append("Cosine-only hits@1=$cosineHits/${cases.size} (rate=${"%.2f".format(cosineRate)}). ")
            append("Boosted hits@1=$boostedHits/${cases.size} (rate=${"%.2f".format(boostedRate)}). ")
            if (recommendsE4b) {
                append("Cosine-only below ${COSINE_ONLY_HIT_RATE_E4B_THRESHOLD}; ")
                append("E4b recommended for semantic-only quality without token assist. ")
            }
            append("Does not authorize product AVAILABLE UI.")
            if (appendM2FollowUp) {
                append(" M2 = on-device MediaPipe.")
            }
        }

        if (enforceBoostedHitBar) {
            require(boostedRate >= BOOSTED_HIT_RATE_TARGET) {
                "Boosted path must clear interim labeled-corpus bar for M1 close."
            }
        }

        return MeaningPdfPageRecallBaselineReport(
            deviceTierId = deviceTierId,
            fixtureCorpusId = MeaningPdfPageRecallCorpus.CORPUS_ID,
            caseCount = cases.size,
            cosineOnlyHitsAt1 = cosineHits,
            boostedHitsAt1 = boostedHits,
            claims = claims,
            recommendsE4bForSemanticOnly = recommendsE4b,
            notes = notes,
        )
    }

    fun topMatchesExpected(
        case: MeaningPdfPageRecallCase,
        mode: MeaningPdfPageRecallRankingMode,
    ): Boolean {
        val ranked = case.candidates
            .map { candidate ->
                val score = when (mode) {
                    MeaningPdfPageRecallRankingMode.COSINE_ONLY ->
                        candidate.injectedCosine.coerceIn(0f, 1f)
                    MeaningPdfPageRecallRankingMode.EVIDENCE_TOKEN_BOOSTED ->
                        MeaningEvidenceTokenBoost.apply(
                            cosine = candidate.injectedCosine,
                            query = case.cue,
                            evidenceText = candidate.evidenceText,
                        ).first
                }
                candidate to score
            }
            .sortedByDescending { it.second }
        val top = ranked.first().first
        return top.assetFileName == case.expectedAssetFileName &&
            top.pageNumber == case.expectedPageNumber
    }
}
