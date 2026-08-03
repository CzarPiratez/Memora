package com.memora.app.application.intelligence

/**
 * Chooses which PDF page to open from a meaning hit (E5b2e).
 *
 * Prefers a cue-best saved page when its embedding score clearly beats the
 * Memory cite; otherwise keeps the cited page (E5b2d) or page-1 fallback.
 */
object ResolveMeaningPdfOpenPage {
    const val FALLBACK_PAGE = 1
    const val MIN_CUE_SCORE = 0.12f
    const val MIN_MARGIN_OVER_CITED = 0.03f
    const val MAX_PAGES_TO_SCORE = 12
    const val MAX_PAGE_CHARS = 480

    fun decide(
        citedPage: Int?,
        scoredPages: List<ScoredPdfPage>,
    ): MeaningPdfOpenPageDecision {
        val cited = citedPage?.takeIf { it > 0 }
        if (scoredPages.isEmpty()) {
            return if (cited != null) {
                MeaningPdfOpenPageDecision(cited, MeaningPdfOpenPageBasis.CITED)
            } else {
                MeaningPdfOpenPageDecision(FALLBACK_PAGE, MeaningPdfOpenPageBasis.FALLBACK)
            }
        }
        val best = scoredPages.maxBy { it.score }
        val citedScore = cited?.let { page ->
            scoredPages.firstOrNull { it.pageNumber == page }?.score
        } ?: 0f
        val cueWins = best.score >= MIN_CUE_SCORE &&
            best.score >= citedScore + MIN_MARGIN_OVER_CITED &&
            (cited == null || best.pageNumber != cited)
        return when {
            cueWins -> MeaningPdfOpenPageDecision(
                pageNumber = best.pageNumber,
                basis = MeaningPdfOpenPageBasis.CUE_BEST,
            )
            cited != null -> MeaningPdfOpenPageDecision(cited, MeaningPdfOpenPageBasis.CITED)
            else -> MeaningPdfOpenPageDecision(FALLBACK_PAGE, MeaningPdfOpenPageBasis.FALLBACK)
        }
    }

    fun truncateForEmbed(text: String): String =
        text.replace(WHITESPACE, " ").trim().take(MAX_PAGE_CHARS)

    private val WHITESPACE = Regex("\\s+")
}

data class ScoredPdfPage(
    val pageNumber: Int,
    val score: Float,
) {
    init {
        require(pageNumber > 0)
        require(score.isFinite())
    }
}

enum class MeaningPdfOpenPageBasis {
    CITED,
    CUE_BEST,
    RANKED_HIT,
    FALLBACK,
}

data class MeaningPdfOpenPageDecision(
    val pageNumber: Int,
    val basis: MeaningPdfOpenPageBasis,
) {
    init {
        require(pageNumber > 0)
    }
}
