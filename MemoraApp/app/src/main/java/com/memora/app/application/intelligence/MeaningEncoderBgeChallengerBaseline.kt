package com.memora.app.application.intelligence

/**
 * Frozen BGE-small challenger measurement from the founder library
 * (2026-09-21). Slice 3 result card. Not a product ranker.
 *
 * Same cue set and unmatched-gold count as [MeaningEncoderUseBaseline].
 * Verdict on this library: challenger_wins (gold@60 11 > 0).
 */
object MeaningEncoderBgeChallengerBaseline {
    const val MEASURED_ON = "2026-09-21"
    const val VECTORS_SCANNED = 3046
    const val UNMATCHED = 5

    /**
     * Collapsed asset ranks for the 12 cues whose gold label was present.
     * Order does not matter. No query text.
     */
    val matchedAssetRanks: List<Int> = listOf(
        13, 19, 5, 11, 4, 90, 3, 6, 1, 13, 2, 14,
    )

    val card: MeaningEncoderBakeOffScorecard =
        MeaningEncoderBakeOffScorecard.ofRanks(
            packId = "onnx-bge-small-en-v1.5",
            matchedAssetRanks = matchedAssetRanks,
            unmatched = UNMATCHED,
            challenger = "onnx-bge-small-en-v1.5",
        )
}
