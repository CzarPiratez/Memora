package com.memora.app.domain.intelligence

/**
 * How completely a meaning result list satisfies the words the person named
 * (defect D-12, Ask Model **P-AND vs P-LIST**).
 *
 * The lexical AND gate keeps meaning results honest, but as an absolute veto it
 * also made paraphrase impossible. On device, `swimming schedule` answered
 * nothing against PDFs that say `swimming timetable`: the meaning model ranked
 * them correctly and the gate then removed every one of them for missing a word
 * the person never needed the file to contain. Embeddings were ordering results
 * they were not allowed to find.
 *
 * The gate is therefore a **tier**, not a veto, and a list has to state which of
 * the person's words its evidence actually contains. This is not a synonym net:
 * UNFYND still never claims `schedule` means `timetable`. It says what it
 * matched and what it could not, and lets the reader judge.
 */
sealed interface RecallPrecision {
    /** Every named word appears in the stored text of every remaining hit. */
    data object Exact : RecallPrecision

    /**
     * At least one remaining hit is missing a named word, so the list is not a
     * complete lexical match.
     *
     * [missing] must reach the person. A partial answer presented as a whole one
     * is worse than an empty one, because it looks like a complete search.
     *
     * When [exactHitsPresent] is true, some files *did* contain every named word
     * and a neighbour that only matched some of them was kept (defect D-20). The
     * banner must not then say nothing saved contains a word an Exact hit has.
     *
     * When [mixedNamedWordFamilies] is true, no remaining file has every named
     * word, but every named word in [matched] still appears on some remaining
     * hit (defect D-21). The banner must not say nothing saved contains those
     * words.
     */
    data class Partial(
        val matched: List<String>,
        val missing: List<String>,
        val exactHitsPresent: Boolean = false,
        val mixedNamedWordFamilies: Boolean = false,
    ) : RecallPrecision {
        init {
            require(matched.isNotEmpty()) { "Partial precision requires a matched word." }
            require(missing.isNotEmpty()) { "Partial precision requires a missing word." }
        }
    }

    /**
     * No saved Memory carried any of the named words. The list is the strongest
     * on-device meaning neighbours that still cleared a floor — not a claim that
     * those words appear, and not a synonym net (`pool` is never asserted to
     * mean `swimming`).
     *
     * [missing] is every word the person named, carried on the tier for the same
     * reason [Partial.missing] is: the banner has to name the miss, and a second
     * derivation of "what was named" at the UI layer is how the query vector and
     * the precision gate drifted apart in defect D-10.
     *
     * A one-word miss stays empty: that is "this word is not in any file", not
     * a paraphrase. Meaning-only requires at least two named words so a missed
     * `silky` cannot surface fashion-adjacent junk.
     */
    data class MeaningOnly(val missing: List<String>) : RecallPrecision {
        init {
            require(missing.size >= MIN_NAMED_WORDS) {
                "Meaning-only needs at least $MIN_NAMED_WORDS named words; a one-word " +
                    "miss is 'this word is not in any file', not a paraphrase."
            }
            require(missing.none { it.isBlank() }) {
                "Meaning-only cannot report a blank word as missing."
            }
        }

        companion object {
            /** @see MeaningOnly for why one word is not enough. */
            const val MIN_NAMED_WORDS = 2
        }
    }
}
