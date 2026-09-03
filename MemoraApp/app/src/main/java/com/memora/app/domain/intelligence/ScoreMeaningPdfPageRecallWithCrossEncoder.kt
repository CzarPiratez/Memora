package com.memora.app.domain.intelligence

/**
 * Scores [MeaningPdfPageRecallCorpus] labeled cases with a cross-encoder head (FC-02 S2).
 *
 * Does not wire Canonical Recall. Does not authorize AVAILABLE.
 */
fun interface CrossEncoderPairScorer {
    fun score(query: String, passage: String): Float
}

data class StageACrossEncoderCaseBreakdown(
    val caseId: String,
    val hitAt1: Boolean,
    val topAssetFileName: String,
    val topPageNumber: Int?,
    val topScore: Float,
    val expectedAssetFileName: String,
    val expectedPageNumber: Int,
)

data class StageACrossEncoderFixtureReport(
    val deviceTierId: String,
    val fixtureCorpusId: String,
    val caseCount: Int,
    val crossEncoderHitsAt1: Int,
    val e5dBoostedBaselineHitsAt1: Int,
    val pairScoreCount: Int,
    val scoreWallMs: Long,
    val caseBreakdown: List<StageACrossEncoderCaseBreakdown>,
    val notes: String,
) {
    init {
        require(deviceTierId.isNotBlank())
        require(fixtureCorpusId.isNotBlank())
        require(caseCount > 0)
        require(crossEncoderHitsAt1 in 0..caseCount)
        require(e5dBoostedBaselineHitsAt1 in 0..caseCount)
        require(pairScoreCount > 0)
        require(scoreWallMs >= 0L)
        require(caseBreakdown.size == caseCount)
        require(notes.isNotBlank())
    }

    val meetsS2Bar: Boolean
        get() = crossEncoderHitsAt1 >= e5dBoostedBaselineHitsAt1 &&
            crossEncoderHitsAt1 == caseCount
}

object ScoreMeaningPdfPageRecallWithCrossEncoder {
    /** E5d-boosted M4 bar on this corpus is 3/3. */
    const val E5D_BOOSTED_BASELINE_HITS_AT_1 = 3

    fun score(
        scorer: CrossEncoderPairScorer,
        labeledCases: List<MeaningPdfPageRecallLabeledCase> =
            MeaningPdfPageRecallCorpus.labeledCases(),
        deviceTierId: String = MeaningPdfPageRecallCorpus.DEVICE_TIER_JVM_UNIT,
    ): StageACrossEncoderFixtureReport {
        require(labeledCases.isNotEmpty())
        require(deviceTierId.isNotBlank())

        val started = System.nanoTime()
        var pairCount = 0
        var hits = 0
        val breakdown = ArrayList<StageACrossEncoderCaseBreakdown>(labeledCases.size)
        labeledCases.forEach { labeled ->
            val ranked = labeled.candidates
                .map { candidate ->
                    pairCount += 1
                    candidate to scorer.score(labeled.cue, candidate.evidenceText)
                }
                .sortedByDescending { it.second }
            val top = ranked.first()
            val hit = top.first.assetFileName == labeled.expectedAssetFileName &&
                top.first.pageNumber == labeled.expectedPageNumber
            if (hit) hits += 1
            breakdown.add(
                StageACrossEncoderCaseBreakdown(
                    caseId = labeled.caseId,
                    hitAt1 = hit,
                    topAssetFileName = top.first.assetFileName,
                    topPageNumber = top.first.pageNumber,
                    topScore = top.second,
                    expectedAssetFileName = labeled.expectedAssetFileName,
                    expectedPageNumber = labeled.expectedPageNumber,
                ),
            )
        }
        val wallMs = ((System.nanoTime() - started) / 1_000_000L).coerceAtLeast(0L)
        return StageACrossEncoderFixtureReport(
            deviceTierId = deviceTierId,
            fixtureCorpusId = MeaningPdfPageRecallCorpus.CORPUS_ID,
            caseCount = labeledCases.size,
            crossEncoderHitsAt1 = hits,
            e5dBoostedBaselineHitsAt1 = E5D_BOOSTED_BASELINE_HITS_AT_1,
            pairScoreCount = pairCount,
            scoreWallMs = wallMs,
            caseBreakdown = breakdown,
            notes = buildString {
                append("FC-02 S2 Stage A cross-encoder fixture on ")
                append(MeaningPdfPageRecallCorpus.CORPUS_ID)
                append(". hits@1=$hits/${labeledCases.size} vs E5d-boosted baseline ")
                append("$E5D_BOOSTED_BASELINE_HITS_AT_1/3. ")
                append("Does not authorize product AVAILABLE.")
            },
        )
    }
}
