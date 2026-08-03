package com.memora.app.domain.intelligence

/**
 * Privacy-safe labeled corpus for meaning PDF page-recall baselines (M1).
 *
 * Texts mirror [MemoraApp/fixtures](../../../../fixtures) open PDFs. No user
 * content. Does not authorize product AVAILABLE claims by itself.
 */
object MeaningPdfPageRecallCorpus {
    const val CORPUS_ID = "meaning-pdf-page-recall-v1"
    const val DEVICE_TIER_JVM_UNIT = "jvm_unit_test"

    /**
     * Compact-model failure pattern (emulator smoke): distractor cosines beat
     * the true cue page until evidence-token boost (+0.35) recovers labeled @1.
     * Injected scores are ordered so cosine-only fails and boost uniquely
     * selects the expected asset/page (not a rival cue-bearing page).
     */
    fun cases(): List<MeaningPdfPageRecallCase> = listOf(
        MeaningPdfPageRecallCase(
            caseId = "mira-5page-p5",
            cue = "mira",
            expectedAssetFileName = "memora-open-5page.pdf",
            expectedPageNumber = 5,
            candidates = listOf(
                candidate("memora-open-5page.pdf", 1, "Page 1 FOXTROT cover sheet", 0.48f),
                candidate("memora-open-5page.pdf", 5, "Page 5 JULIET meet mira closing", 0.22f),
                candidate("memora-open-3page.pdf", 3, "Page 3 ECHO meet mira follow-up", 0.18f),
                candidate("memora-open-2page.pdf", 2, "Memora page two BRAVO meet mira", 0.15f),
                candidate(
                    "Screenshot_memora_note.png",
                    pageNumber = null,
                    text = "Screenshot note",
                    cosine = 0.45f,
                ),
            ),
        ),
        MeaningPdfPageRecallCase(
            caseId = "boarding-3page-p2",
            cue = "boarding",
            expectedAssetFileName = "memora-open-3page.pdf",
            expectedPageNumber = 2,
            candidates = listOf(
                candidate("memora-open-3page.pdf", 1, "Page 1 CHARLIE receipt total", 0.40f),
                candidate("memora-open-3page.pdf", 2, "Page 2 DELTA boarding pass gate", 0.28f),
                candidate("memora-open-5page.pdf", 1, "Page 1 FOXTROT cover sheet", 0.38f),
            ),
        ),
        MeaningPdfPageRecallCase(
            caseId = "invoice-5page-p2",
            cue = "invoice",
            expectedAssetFileName = "memora-open-5page.pdf",
            expectedPageNumber = 2,
            candidates = listOf(
                candidate("memora-open-5page.pdf", 1, "Page 1 FOXTROT cover sheet", 0.45f),
                candidate("memora-open-5page.pdf", 2, "Page 2 GOLF invoice number", 0.30f),
                candidate("memora-open-3page.pdf", 1, "Page 1 CHARLIE receipt total", 0.36f),
            ),
        ),
    )

    private fun candidate(
        assetFileName: String,
        pageNumber: Int?,
        text: String,
        cosine: Float,
    ) = MeaningPdfPageRecallCandidate(
        assetFileName = assetFileName,
        pageNumber = pageNumber,
        evidenceText = text,
        injectedCosine = cosine,
    )
}

data class MeaningPdfPageRecallCase(
    val caseId: String,
    val cue: String,
    val expectedAssetFileName: String,
    val expectedPageNumber: Int,
    val candidates: List<MeaningPdfPageRecallCandidate>,
) {
    init {
        require(caseId.isNotBlank())
        require(cue.isNotBlank())
        require(expectedAssetFileName.isNotBlank())
        require(expectedPageNumber > 0)
        require(candidates.isNotEmpty())
    }
}

data class MeaningPdfPageRecallCandidate(
    val assetFileName: String,
    val pageNumber: Int?,
    val evidenceText: String,
    val injectedCosine: Float,
) {
    init {
        require(assetFileName.isNotBlank())
        require(evidenceText.isNotBlank())
        require(injectedCosine.isFinite())
        require(pageNumber == null || pageNumber > 0)
    }
}
