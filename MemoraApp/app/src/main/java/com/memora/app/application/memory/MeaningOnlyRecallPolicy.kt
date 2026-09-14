package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit

/**
 * Zero-overlap paraphrase inside Canonical Recall (Ask Model **P-EVIDENCE**,
 * PROGRAM_STATE meaning-only tier).
 *
 * When no named word appears in any candidate, D-12 used to empty the list.
 * Embeddings had already ranked a neighbour; the lexical tier then forbade
 * the product from showing it. That is the remaining hole after D-12/D-15:
 * `kids water lessons` against a swimming-timetable PDF that contains neither
 * word.
 *
 * This is not a synonym net. Admission never claims two words mean the same
 * thing. It keeps a short, high-cosine band and leaves honesty to the banner.
 *
 * A single named word with zero overlap stays empty: that is "this word is
 * not in any file", which keyword Find already answers, not a paraphrase.
 */
object MeaningOnlyRecallPolicy {
    const val MIN_NAMED_WORDS = 2
    const val MIN_COSINE = 0.32f
    const val MAX_HITS = 3
    const val RELATIVE_SCORE_GAP = 0.12f

    fun select(hits: List<MeaningSearchHit>, namedWordCount: Int): List<MeaningSearchHit> {
        if (namedWordCount < MIN_NAMED_WORDS || hits.isEmpty()) return emptyList()
        val strong = hits.filter { it.score >= MIN_COSINE }.sortedByDescending { it.score }
        if (strong.isEmpty()) return emptyList()
        val top = strong.first().score
        return strong.filter { top - it.score <= RELATIVE_SCORE_GAP }.take(MAX_HITS)
    }
}
