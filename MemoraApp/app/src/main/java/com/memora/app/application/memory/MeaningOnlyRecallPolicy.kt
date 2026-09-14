package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.RecallPrecision

/**
 * Zero-overlap paraphrase inside Canonical Recall (Ask Model **P-MEANING-ONLY**,
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
 *
 * ## Judged on similarity alone
 *
 * The floor reads [MeaningSearchHit.cosine], never [MeaningSearchHit.score],
 * and admission strips the word assist from what it returns. Token boost
 * derives its tokens from the whole query while this tier derives them from
 * [com.memora.app.domain.intelligence.MeaningRecallCue.contentTokens], which
 * drops the words a TIME constraint owns. So `recent water lessons` against a
 * file that merely says `recent` arrived here already carrying `+0.35` — enough
 * to clear the floor on a word the cue never asked the file to contain, and
 * enough to make the card say "helped by a word you typed" underneath a banner
 * saying none of those words were found. On this tier, by definition, no named
 * word is in the file, so no assist may survive into rank or into Why.
 */
object MeaningOnlyRecallPolicy {
    /** @see RecallPrecision.MeaningOnly.MIN_NAMED_WORDS */
    const val MIN_NAMED_WORDS = RecallPrecision.MeaningOnly.MIN_NAMED_WORDS

    /**
     * Similarity a neighbour must reach on its own.
     *
     * **Not measured.** Chosen from USE behaviour on the founder corpus and not
     * yet calibrated against a scored set, so it is the first thing to revisit
     * when this tier is wrong on device. It is deliberately well above
     * [com.memora.app.application.intelligence.SearchAssetMemoriesByMeaning.MIN_CANDIDATE_SCORE]:
     * the candidate floor only excludes noise, while this one has to be
     * strong enough to justify showing a file that contains none of the words.
     */
    const val MIN_COSINE = 0.32f

    const val MAX_HITS = 3

    /** A neighbour far below the best one is a different subject, not a band. */
    const val RELATIVE_SCORE_GAP = 0.12f

    /**
     * @param namedWordCount how many words the cue named, from the same
     *   derivation the precision tier used.
     * @return the admitted band with the word assist removed, or empty.
     */
    fun select(hits: List<MeaningSearchHit>, namedWordCount: Int): List<MeaningSearchHit> {
        if (namedWordCount < MIN_NAMED_WORDS || hits.isEmpty()) return emptyList()
        val strong = hits
            .filter { it.cosine >= MIN_COSINE }
            .sortedByDescending { it.cosine }
        if (strong.isEmpty()) return emptyList()
        val top = strong.first().cosine
        return strong
            .filter { top - it.cosine <= RELATIVE_SCORE_GAP }
            .take(MAX_HITS)
            .map { it.copy(score = it.cosine, evidenceTokenBoosted = false) }
    }
}
