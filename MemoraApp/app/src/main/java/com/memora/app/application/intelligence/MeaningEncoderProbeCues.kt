package com.memora.app.application.intelligence

/**
 * Founder-library cues for the D-20 encoder probe. Gold is a **label/key
 * substring**, not a synonym. Exact-token cues keep the typed word as gold so
 * we can see whether USE ranks that file highly — not whether Find shows it.
 *
 * Probe needles are reused by live [MeaningSearchGoldLocator] (D-22) so
 * gold membership on Find uses the same label/key match. Still does not
 * change product ranking.
 */
object MeaningEncoderProbeCues {
    private val swimmingGold = listOf("timetable", "swimming-tt", "swimming_tt", "swimming tt")

    val DEFAULT: List<MeaningEncoderProbeCue> = listOf(
        MeaningEncoderProbeCue("swimming schedule", goldSubstrings = swimmingGold),
        MeaningEncoderProbeCue("when are the swimming classes", goldSubstrings = swimmingGold),
        MeaningEncoderProbeCue(
            "when are the swimming classes for grade 2",
            goldSubstrings = swimmingGold,
        ),
        MeaningEncoderProbeCue("Wednesday swimming timings", goldSubstrings = swimmingGold),
        MeaningEncoderProbeCue("school swimming timetable", goldSubstrings = swimmingGold),
        MeaningEncoderProbeCue("kids water lessons", goldSubstrings = swimmingGold),
        MeaningEncoderProbeCue("pool timetable", goldSubstrings = swimmingGold),
        MeaningEncoderProbeCue("swimming timetable", goldSubstrings = swimmingGold),
        MeaningEncoderProbeCue("bill payment", goldSubstrings = listOf("bill")),
        MeaningEncoderProbeCue("protein purchase", goldSubstrings = listOf("protein")),
        MeaningEncoderProbeCue("passport", goldSubstrings = listOf("passport")),
        MeaningEncoderProbeCue("Aadhaar", goldSubstrings = listOf("aadhaar", "aadhar")),
        MeaningEncoderProbeCue("Illahi Bagh welfare", goldSubstrings = listOf("illahi")),
        MeaningEncoderProbeCue("bank information", goldSubstrings = listOf("bank")),
        MeaningEncoderProbeCue("invoice", goldSubstrings = listOf("invoice")),
        MeaningEncoderProbeCue("wifi password", goldSubstrings = listOf("wifi", "wi-fi", "wi fi")),
        MeaningEncoderProbeCue(
            "show me the files with swimming timetables",
            goldSubstrings = swimmingGold,
        ),
    )
}

data class MeaningEncoderProbeCue(
    val query: String,
    val goldSubstrings: List<String> = emptyList(),
) {
    init {
        require(query.isNotBlank())
        require(goldSubstrings.all { it.isNotBlank() })
    }
}
