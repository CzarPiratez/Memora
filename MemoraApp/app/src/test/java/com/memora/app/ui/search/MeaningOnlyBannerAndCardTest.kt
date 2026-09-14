package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.application.memory.AnchorAwareMeaningRecallRanking
import com.memora.app.application.memory.EmptyMemoryRepositoryDelegate
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.RecallPrecision
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Banner and card must tell the same story on the meaning-only tier.
 *
 * Defect D-17 was a list banner stating an honest miss while the card
 * underneath quietly argued the opposite. The meaning-only tier is the same
 * shape of risk, with more room for it: by construction none of the person's
 * words are in the file, so every claim the card makes about words has to be
 * silent.
 *
 * These run the real ranking stage rather than hand-built precision, because
 * the two ways the card used to contradict the banner — a word-assist boost
 * surviving admission, and an unrelated quote landing in the evidence slot —
 * are both produced by the pipeline, not by the copy.
 */
class MeaningOnlyBannerAndCardTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun the_banner_names_the_missing_words_the_gate_actually_required() = runBlocking {
        val ranked = rank(
            query = "recent kids water lessons",
            summaryText = "Grade 2 Swimming Timetable 2026, recent revision",
        )

        val precision = ranked.precision as RecallPrecision.MeaningOnly
        // `recent` is a TIME constraint, not something the file had to contain,
        // so the banner must not accuse the library of missing it.
        assertEquals(listOf("kids", "water", "lessons"), precision.missing)
        val banner = MeaningSearchCopy.meaningOnlyBody(precision.missing)
        assertTrue(banner.contains("\"kids\", \"water\" and \"lessons\""))
        assertFalse(banner.contains("recent"))
    }

    @Test
    fun the_card_never_claims_a_word_you_typed_helped_find_it() = runBlocking {
        val ranked = rank(
            query = "recent kids water lessons",
            summaryText = "Grade 2 Swimming Timetable 2026, recent revision",
        )

        val why = CanonicalRecallWhyCopy.present(ranked.hits.single(), "recent kids water lessons")
        assertEquals(CanonicalRecallWhyCopy.FOUND_BY_MEANING, why.howFound)
        assertFalse(why.spokenText.contains("helped by a word you typed"))
    }

    /**
     * Nothing in the file justifies the hit lexically, so the evidence slot has
     * to stay empty. Quoting the opening clause would read as the line that
     * matched, one screen below a banner saying no word matched.
     */
    @Test
    fun the_card_quotes_no_line_when_no_named_word_is_in_the_file() = runBlocking {
        val ranked = rank(
            query = "kids water lessons",
            summaryText = "Grade 2 Swimming Timetable 2026 PERIOD TIME MON TUE",
        )

        val why = CanonicalRecallWhyCopy.present(ranked.hits.single(), "kids water lessons")
        assertNull(why.citedLine)
        assertTrue(why.subject.isNotBlank())
        assertFalse(why.spokenText.contains("\""))
    }

    /** A partial list still earns its quote: one of the words really is there. */
    @Test
    fun a_partial_card_still_quotes_the_line_that_matched() = runBlocking {
        val ranked = rank(
            query = "pool timetable",
            summaryText = "Grade 2 Swimming Timetable 2026 PERIOD TIME",
        )

        assertTrue(ranked.precision is RecallPrecision.Partial)
        val why = CanonicalRecallWhyCopy.present(ranked.hits.single(), "pool timetable")
        assertTrue(why.citedLine!!.contains("Timetable", ignoreCase = true))
    }

    private suspend fun rank(query: String, summaryText: String): MeaningSearchOutcome.Matches {
        val outcome = MeaningSearchOutcome.Matches(
            query = query,
            hits = listOf(
                MeaningSearchHit(
                    revisionId = MemoryRevisionId("rev-timetable"),
                    memoryId = MemoryId("mem"),
                    sourceId = SourceId("src"),
                    sourceAssetKey = SourceAssetKey("asset"),
                    assetType = AssetType.PDF,
                    label = "Grade-2-Swimming-TT-2026.pdf",
                    summaryText = summaryText,
                    score = 0.58f,
                    model = model,
                ),
            ),
            limitReached = false,
            model = model,
        )
        return AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = query,
            memoryRepository = NoAnchorRepository(),
        ) as MeaningSearchOutcome.Matches
    }

    private class NoAnchorRepository : MemoryRepository by EmptyMemoryRepositoryDelegate()
}
