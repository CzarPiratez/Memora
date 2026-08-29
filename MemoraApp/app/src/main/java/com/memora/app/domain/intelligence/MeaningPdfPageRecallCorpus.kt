package com.memora.app.domain.intelligence

/**
 * Privacy-safe labeled corpus for meaning PDF page-recall baselines (M1/M2).
 *
 * Texts mirror [MemoraApp/fixtures](../../../../fixtures) open PDFs. No user
 * content. Does not authorize product AVAILABLE claims by itself.
 */
object MeaningPdfPageRecallCorpus {
    const val CORPUS_ID = "meaning-pdf-page-recall-v1"
    const val DEVICE_TIER_JVM_UNIT = "jvm_unit_test"
    const val DEVICE_TIER_EMULATOR_MEDIUM_PHONE = "emulator_medium_phone"
    /** Physical midrange host class (LOCAL_AI_BENCHMARK_PLAN / M4). */
    const val DEVICE_TIER_MIDRANGE_ARM64 = "midrange_arm64"

    /** Text-only labeled cases for live embedding (M2). */
    fun labeledCases(): List<MeaningPdfPageRecallLabeledCase> = listOf(
        MeaningPdfPageRecallLabeledCase(
            caseId = "mira-5page-p5",
            cue = "mira",
            expectedAssetFileName = "memora-open-5page.pdf",
            expectedPageNumber = 5,
            candidates = listOf(
                textCandidate("memora-open-5page.pdf", 1, "Page 1 FOXTROT cover sheet"),
                textCandidate("memora-open-5page.pdf", 5, "Page 5 JULIET meet mira closing"),
                textCandidate("memora-open-3page.pdf", 3, "Page 3 ECHO meet mira follow-up"),
                textCandidate("memora-open-2page.pdf", 2, "Memora page two BRAVO meet mira"),
                textCandidate("Screenshot_memora_note.png", null, "Screenshot note"),
            ),
        ),
        MeaningPdfPageRecallLabeledCase(
            caseId = "boarding-3page-p2",
            cue = "boarding",
            expectedAssetFileName = "memora-open-3page.pdf",
            expectedPageNumber = 2,
            candidates = listOf(
                textCandidate("memora-open-3page.pdf", 1, "Page 1 CHARLIE receipt total"),
                textCandidate("memora-open-3page.pdf", 2, "Page 2 DELTA boarding pass gate"),
                textCandidate("memora-open-5page.pdf", 1, "Page 1 FOXTROT cover sheet"),
            ),
        ),
        MeaningPdfPageRecallLabeledCase(
            caseId = "invoice-5page-p2",
            cue = "invoice",
            expectedAssetFileName = "memora-open-5page.pdf",
            expectedPageNumber = 2,
            candidates = listOf(
                textCandidate("memora-open-5page.pdf", 1, "Page 1 FOXTROT cover sheet"),
                textCandidate("memora-open-5page.pdf", 2, "Page 2 GOLF invoice number"),
                textCandidate("memora-open-3page.pdf", 1, "Page 1 CHARLIE receipt total"),
            ),
        ),
    )

    /**
     * M1 JVM injected compact-failure pattern: distractor cosines beat the true
     * cue page until evidence-token boost recovers labeled @1.
     */
    fun cases(): List<MeaningPdfPageRecallCase> = listOf(
        MeaningPdfPageRecallCase(
            caseId = "mira-5page-p5",
            cue = "mira",
            expectedAssetFileName = "memora-open-5page.pdf",
            expectedPageNumber = 5,
            candidates = listOf(
                scoredCandidate("memora-open-5page.pdf", 1, "Page 1 FOXTROT cover sheet", 0.48f),
                scoredCandidate("memora-open-5page.pdf", 5, "Page 5 JULIET meet mira closing", 0.22f),
                scoredCandidate("memora-open-3page.pdf", 3, "Page 3 ECHO meet mira follow-up", 0.18f),
                scoredCandidate("memora-open-2page.pdf", 2, "Memora page two BRAVO meet mira", 0.15f),
                scoredCandidate("Screenshot_memora_note.png", null, "Screenshot note", 0.45f),
            ),
        ),
        MeaningPdfPageRecallCase(
            caseId = "boarding-3page-p2",
            cue = "boarding",
            expectedAssetFileName = "memora-open-3page.pdf",
            expectedPageNumber = 2,
            candidates = listOf(
                scoredCandidate("memora-open-3page.pdf", 1, "Page 1 CHARLIE receipt total", 0.40f),
                scoredCandidate("memora-open-3page.pdf", 2, "Page 2 DELTA boarding pass gate", 0.28f),
                scoredCandidate("memora-open-5page.pdf", 1, "Page 1 FOXTROT cover sheet", 0.38f),
            ),
        ),
        MeaningPdfPageRecallCase(
            caseId = "invoice-5page-p2",
            cue = "invoice",
            expectedAssetFileName = "memora-open-5page.pdf",
            expectedPageNumber = 2,
            candidates = listOf(
                scoredCandidate("memora-open-5page.pdf", 1, "Page 1 FOXTROT cover sheet", 0.45f),
                scoredCandidate("memora-open-5page.pdf", 2, "Page 2 GOLF invoice number", 0.30f),
                scoredCandidate("memora-open-3page.pdf", 1, "Page 1 CHARLIE receipt total", 0.36f),
            ),
        ),
    )

    private fun textCandidate(
        assetFileName: String,
        pageNumber: Int?,
        text: String,
    ) = MeaningPdfPageRecallTextCandidate(
        assetFileName = assetFileName,
        pageNumber = pageNumber,
        evidenceText = text,
    )

    private fun scoredCandidate(
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

data class MeaningPdfPageRecallLabeledCase(
    val caseId: String,
    val cue: String,
    val expectedAssetFileName: String,
    val expectedPageNumber: Int,
    val candidates: List<MeaningPdfPageRecallTextCandidate>,
) {
    init {
        require(caseId.isNotBlank())
        require(cue.isNotBlank())
        require(expectedAssetFileName.isNotBlank())
        require(expectedPageNumber > 0)
        require(candidates.isNotEmpty())
    }
}

data class MeaningPdfPageRecallTextCandidate(
    val assetFileName: String,
    val pageNumber: Int?,
    val evidenceText: String,
) {
    init {
        require(assetFileName.isNotBlank())
        require(evidenceText.isNotBlank())
        require(pageNumber == null || pageNumber > 0)
    }
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
