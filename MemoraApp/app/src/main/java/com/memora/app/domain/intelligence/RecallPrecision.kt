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
    /** Every named word appears in the stored text of every hit. */
    data object Exact : RecallPrecision

    /**
     * No saved Memory contained all of the named words, so this list is the best
     * lexical tier available.
     *
     * [missing] must reach the person. A partial answer presented as a whole one
     * is worse than an empty one, because it looks like a complete search.
     */
    data class Partial(
        val matched: List<String>,
        val missing: List<String>,
    ) : RecallPrecision {
        init {
            require(matched.isNotEmpty()) { "Partial precision requires a matched word." }
            require(missing.isNotEmpty()) { "Partial precision requires a missing word." }
        }
    }
}
