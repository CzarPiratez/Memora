package com.memora.app.application.intelligence

/**
 * Frozen USE measurement from the founder library (2026-09-21). Slice 3
 * control card. Not a product ranker. Not a per-cue table.
 *
 * Unmatched gold (null rank) is a locator/index fact, not an encoder
 * rank, and is not a Find occupancy ticket.
 */
object MeaningEncoderUseBaseline {
    const val MEASURED_ON = "2026-09-21"
    const val VECTORS_SCANNED = 3046
    const val UNMATCHED = 5

    /**
     * Collapsed asset ranks for the 12 cues whose gold label was present
     * in the index. Order does not matter. No query text.
     */
    val matchedAssetRanks: List<Int> = listOf(
        385, 695, 421, 197, 286, 847, 422, 305, 78, 800, 179, 320,
    )

    val card: MeaningEncoderBakeOffScorecard =
        MeaningEncoderBakeOffScorecard.ofRanks(
            packId = MeaningEncoderBakeOffScorecard.CURRENT_PACK_ID,
            matchedAssetRanks = matchedAssetRanks,
            unmatched = UNMATCHED,
            challenger = MeaningEncoderBakeOffScorecard.CHALLENGER_NOT_INSTALLED,
        )
}
