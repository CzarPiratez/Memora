package com.memora.app.domain.memory

/**
 * Honest indexed / pending / blocked inventory for meaning-search corpus (FC-04).
 *
 * Counts reflect Memora-owned Room state on this phone only — not a marketing
 * AVAILABLE claim or cloud corpus.
 */
data class CorpusCompletenessCounts(
    val memoriesReady: Int,
    val memoriesPendingAssembly: Int,
    val meaningSummaryIndexed: Int,
    val meaningEvidenceIndexed: Int,
    val meaningIndexPending: Int,
) {
    init {
        require(memoriesReady >= 0)
        require(memoriesPendingAssembly >= 0)
        require(meaningSummaryIndexed >= 0)
        require(meaningEvidenceIndexed >= 0)
        require(meaningIndexPending >= 0)
    }

    val meaningVectorsIndexed: Int
        get() = meaningSummaryIndexed + meaningEvidenceIndexed
}

/** Hard prerequisite missing before meaning search can be useful. */
sealed interface CorpusCompletenessBlocked {
    /** Saved extraction facts exist but no READY Asset Memory yet. */
    data object BuildMemoriesFirst : CorpusCompletenessBlocked

    /** READY memories exist but the meaning index is empty. */
    data object BuildMeaningIndex : CorpusCompletenessBlocked
}

data class CorpusCompletenessSnapshot(
    val counts: CorpusCompletenessCounts,
    val blocked: CorpusCompletenessBlocked?,
)
