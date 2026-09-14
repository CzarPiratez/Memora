package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.RecallPrecision
import com.memora.app.domain.memory.MemoryAnchor
import com.memora.app.domain.memory.MemoryAnchorId
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.MemoryText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorAwareMeaningRecallRankingTest {
    private val model = ModelVersionIdentity("test-model", "1")

    /**
     * T10: an explicit time constraint must not switch precision off. `2024` is
     * a constraint carried by a TIME anchor; `notes` is content and still has to
     * appear in stored text.
     */
    @Test
    fun apply_explicit_time_still_requires_the_content_word() = runBlocking {
        val keep = MemoryRevisionId("rev-notes")
        val dropNoContent = MemoryRevisionId("rev-no-notes")
        val outcome = MeaningSearchOutcome.Matches(
            query = "notes in 2024",
            hits = listOf(
                // Right date, wrong document: nothing here says "notes".
                hit(dropNoContent, 0.95f, summaryText = "Bus discipline rules"),
                hit(keep, 0.4f, summaryText = "Parent evening notes"),
            ),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "notes in 2024",
            memoryRepository = FakeAnchorRepository(
                mapOf(
                    keep to listOf(timeAnchor("Date taken: 2024-03-01")),
                    dropNoContent to listOf(timeAnchor("Date taken: 2024-05-02")),
                ),
            ),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(keep), filtered.hits.map { it.revisionId })
    }

    /** T10, advisory half: `recent` must not disable precision on `silky`. */
    @Test
    fun apply_advisory_time_still_requires_the_content_word() = runBlocking {
        val keep = MemoryRevisionId("rev-silky")
        val drop = MemoryRevisionId("rev-bus")
        val outcome = MeaningSearchOutcome.Matches(
            query = "recent files with silky",
            hits = listOf(
                hit(drop, 0.95f, label = "Bus.pdf", summaryText = "Bus Discipline Rules"),
                hit(keep, 0.3f, label = "Spell.pdf", summaryText = "anchor silky wreck cook"),
            ),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "recent files with silky",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(keep), filtered.hits.map { it.revisionId })
    }

    /** The time words themselves are never required in stored text (T10). */
    @Test
    fun apply_does_not_require_the_time_words_in_evidence() = runBlocking {
        val keep = MemoryRevisionId("rev-notes")
        val outcome = MeaningSearchOutcome.Matches(
            query = "notes in 2024",
            hits = listOf(hit(keep, 0.4f, summaryText = "Parent evening notes")),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "notes in 2024",
            memoryRepository = FakeAnchorRepository(
                mapOf(keep to listOf(timeAnchor("Date taken: 2024-03-01"))),
            ),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(keep), filtered.hits.map { it.revisionId })
    }

    @Test
    fun apply_explicit_time_excludes_non_matching_anchor_hits() = runBlocking {
        val revMatch = MemoryRevisionId("rev-match")
        val revMiss = MemoryRevisionId("rev-miss")
        val outcome = MeaningSearchOutcome.Matches(
            query = "notes in 2024",
            hits = listOf(
                // Both carry the content word, so only the anchor can separate them.
                hit(revMiss, 0.95f, summaryText = "Sports day notes"),
                hit(revMatch, 0.9f, summaryText = "Parent evening notes"),
            ),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "notes in 2024",
            memoryRepository = FakeAnchorRepository(
                mapOf(
                    revMatch to listOf(timeAnchor("Date taken: 2024-03-01")),
                    revMiss to listOf(timeAnchor("Date taken: 2022-01-01")),
                ),
            ),
        ) as MeaningSearchOutcome.Matches

        assertEquals(1, filtered.hits.size)
        assertEquals(revMatch, filtered.hits.single().revisionId)
    }

    /**
     * T11: a plain cue carries no TOPIC constraint, so the ranker must not pay
     * for an anchor lookup on every search.
     */
    @Test
    fun apply_plain_query_does_not_reach_for_anchors() = runBlocking {
        val repository = FakeAnchorRepository()
        val outcome = MeaningSearchOutcome.Matches(
            query = "silky",
            hits = listOf(hit(MemoryRevisionId("rev-1"), 0.4f, summaryText = "anchor silky wreck")),
            limitReached = false,
            model = model,
        )

        AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "silky",
            memoryRepository = repository,
        )

        assertEquals(0, repository.signatureAnchorLookups)
    }

    @Test
    fun apply_token_boost_promotes_hit_that_contains_cue_token() = runBlocking {
        val foxtrot = MemoryRevisionId("rev-5")
        val miraPage = MemoryRevisionId("rev-3")
        val outcome = MeaningSearchOutcome.Matches(
            query = "mira",
            hits = listOf(
                hit(
                    revisionId = foxtrot,
                    score = 0.4f,
                    label = "memora-open-5page.pdf",
                    summaryText = "Page 1 FOXTROT cover sheet",
                    rankedPdfPageNumber = 1,
                ),
                hit(
                    revisionId = miraPage,
                    score = 0.2f,
                    label = "memora-open-3page.pdf",
                    summaryText = "Page 3 ECHO meet mira follow-up",
                    rankedPdfPageNumber = 3,
                ),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "mira",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        // Precision (MF-1): foxtrot lacks "mira" and is dropped; mira page remains + boosted.
        assertEquals(1, ranked.hits.size)
        val top = ranked.hits.single()
        assertEquals("memora-open-3page.pdf", top.label)
        assertEquals(3, top.rankedPdfPageNumber)
        assertTrue(top.evidenceTokenBoosted)
        assertTrue(top.score > 0.2f)
    }

    /**
     * Regression guard from the Stage A CE fix: when the anchor stage runs but
     * every anchor is neutral, it must preserve the incoming rank order rather
     * than resorting on score. Driven by an advisory TIME cue, since TOPIC is no
     * longer advisory on ordinary queries (T11).
     */
    @Test
    fun apply_preserves_rank_order_when_anchors_are_neutral() = runBlocking {
        val invoiceMatch = MemoryRevisionId("rev-invoice")
        val other = MemoryRevisionId("rev-other")
        val outcome = MeaningSearchOutcome.Matches(
            query = "recent invoice",
            hits = listOf(
                hit(
                    invoiceMatch,
                    0.8f,
                    label = "memora-open-5page.pdf",
                    summaryText = "GOLF invoice number on page two",
                ),
                hit(
                    other,
                    0.7f,
                    label = "memora-open-3page.pdf",
                    summaryText = "cover sheet mentions invoice briefly",
                ),
            ),
            limitReached = false,
            model = model,
        )

        val repository = FakeAnchorRepository(
            mapOf(
                invoiceMatch to listOf(
                    topicAnchor("memora-open-5page.pdf", MemoryEvidenceId("e-topic-5")),
                ),
                other to listOf(
                    topicAnchor("memora-open-3page.pdf", MemoryEvidenceId("e-topic-3")),
                ),
            ),
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "recent invoice",
            memoryRepository = repository,
        ) as MeaningSearchOutcome.Matches

        assertEquals(1, repository.signatureAnchorLookups)
        assertEquals(2, ranked.hits.size)
        assertEquals(invoiceMatch, ranked.hits.first().revisionId)
    }

    @Test
    fun apply_lexical_and_filter_excludes_semantic_hits_missing_explicit_tokens() = runBlocking {
        val scanOnly = MemoryRevisionId("rev-scan")
        val scanAndSilky = MemoryRevisionId("rev-both")
        val outcome = MeaningSearchOutcome.Matches(
            query = "files with scan and silky",
            hits = listOf(
                hit(
                    revisionId = scanOnly,
                    score = 0.9f,
                    label = "List-B.pdf",
                    summaryText = "Spelling list with scan words only",
                ),
                hit(
                    revisionId = scanAndSilky,
                    score = 0.7f,
                    label = "List-A.pdf",
                    summaryText = "Page 2 scan and silky vocabulary practice",
                ),
            ),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "files with scan and silky",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(1, filtered.hits.size)
        assertEquals(scanAndSilky, filtered.hits.single().revisionId)
    }

    @Test
    fun apply_without_time_or_topic_cue_returns_boost_only() = runBlocking {
        val outcome = MeaningSearchOutcome.Matches(
            query = "wifi",
            hits = listOf(hit(MemoryRevisionId("rev-1"), 0.5f, summaryText = "office wifi password card")),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "wifi",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(outcome.hits.single().revisionId, filtered.hits.single().revisionId)
        assertTrue(filtered.hits.single().evidenceTokenBoosted)
    }

    @Test
    fun apply_single_token_drops_hits_missing_cue_word() = runBlocking {
        val keep = MemoryRevisionId("rev-silky")
        val drop = MemoryRevisionId("rev-bus")
        val outcome = MeaningSearchOutcome.Matches(
            query = "silky",
            hits = listOf(
                hit(drop, 0.95f, label = "Bus.pdf", summaryText = "Bus Discipline Rules"),
                hit(keep, 0.4f, label = "Spell.pdf", summaryText = "anchor silky wreck cook"),
            ),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "silky",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(keep), filtered.hits.map { it.revisionId })
    }

    @Test
    fun apply_nl_question_still_precision_filters_on_content_token() = runBlocking {
        val keep = MemoryRevisionId("rev-silky")
        val drop = MemoryRevisionId("rev-urdu")
        val outcome = MeaningSearchOutcome.Matches(
            query = "which file has silky in it",
            hits = listOf(
                hit(drop, 0.9f, summaryText = "mock urdu paper without the english cue"),
                hit(keep, 0.3f, summaryText = "Irregular consonants school anchor silky wreck"),
            ),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "which file has silky in it",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(keep), filtered.hits.map { it.revisionId })
    }

    @Test
    fun apply_show_me_timetables_paraphrase_keeps_singular_evidence() = runBlocking {
        val keep = MemoryRevisionId("rev-tt")
        val drop = MemoryRevisionId("rev-bus")
        val outcome = MeaningSearchOutcome.Matches(
            query = "Show me the files with swimming timetables",
            hits = listOf(
                hit(drop, 0.92f, summaryText = "Bus Discipline Rules for Students"),
                hit(keep, 0.28f, summaryText = "Year 4 swimming timetable Monday to Friday"),
            ),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "Show me the files with swimming timetables",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(keep), filtered.hits.map { it.revisionId })
    }

    @Test
    fun apply_scan_in_other_saved_page_still_keeps_the_asset() = runBlocking {
        val keep = MemoryRevisionId("rev-scan")
        val drop = MemoryRevisionId("rev-bus")
        val outcome = MeaningSearchOutcome.Matches(
            query = "scan",
            hits = listOf(
                hit(drop, 0.9f, label = "Bus.pdf", summaryText = "Bus Discipline Rules"),
                hit(
                    keep,
                    0.31f,
                    label = "Homework.pdf",
                    summaryText = "Page 1 cover sheet without the body word",
                    precisionText = "Page 1 cover sheet without the body word Please scan this form",
                ),
            ),
            limitReached = false,
            model = model,
        )

        val filtered = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "scan",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(keep), filtered.hits.map { it.revisionId })
    }

    /**
     * D-12, the device case. `swimming schedule` must still reach a PDF that
     * says `swimming timetable`, and must state that nothing contained
     * `schedule`. UNFYND never claims the two words are synonyms.
     */
    @Test
    fun apply_falls_back_to_a_partial_tier_when_no_hit_has_every_word() = runBlocking {
        val timetable = MemoryRevisionId("rev-timetable")
        val outcome = MeaningSearchOutcome.Matches(
            query = "swimming schedule",
            hits = listOf(
                hit(
                    timetable,
                    0.6f,
                    label = "Grade-2-Swimming-TT-2026.pdf",
                    summaryText = "Grade 2 Swimming Timetable 2026 PERIOD TIME MON TUE",
                ),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "swimming schedule",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(timetable), ranked.hits.map { it.revisionId })
        assertEquals(
            RecallPrecision.Partial(matched = listOf("swimming"), missing = listOf("schedule")),
            ranked.precision,
        )
    }

    /** An exact tier wins outright; a partial hit never dilutes a complete one. */
    @Test
    fun apply_prefers_the_exact_tier_and_drops_partial_hits() = runBlocking {
        val exact = MemoryRevisionId("rev-exact")
        val partial = MemoryRevisionId("rev-partial")
        val outcome = MeaningSearchOutcome.Matches(
            query = "swimming schedule",
            hits = listOf(
                hit(partial, 0.9f, label = "TT.pdf", summaryText = "Swimming timetable"),
                hit(exact, 0.2f, label = "Sched.pdf", summaryText = "Swimming schedule term 2"),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "swimming schedule",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(exact), ranked.hits.map { it.revisionId })
        assertEquals(RecallPrecision.Exact, ranked.precision)
    }

    /** Matching more of the person's words is the better partial answer. */
    @Test
    fun apply_prefers_the_tier_that_matches_more_of_the_named_words() = runBlocking {
        val deeper = MemoryRevisionId("rev-deeper")
        val shallower = MemoryRevisionId("rev-shallower")
        val outcome = MeaningSearchOutcome.Matches(
            query = "grade swimming schedule",
            hits = listOf(
                hit(shallower, 0.9f, label = "A.pdf", summaryText = "Swimming lessons"),
                hit(deeper, 0.1f, label = "B.pdf", summaryText = "Grade 2 swimming timetable"),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "grade swimming schedule",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(deeper), ranked.hits.map { it.revisionId })
        assertEquals(
            RecallPrecision.Partial(
                matched = listOf("grade", "swimming"),
                missing = listOf("schedule"),
            ),
            ranked.precision,
        )
    }

    /** A weak cosine neighbour with no named word is still an honest empty. */
    @Test
    fun apply_returns_empty_when_no_hit_clears_the_meaning_only_floor() = runBlocking {
        val outcome = MeaningSearchOutcome.Matches(
            query = "swimming schedule",
            hits = listOf(hit(MemoryRevisionId("rev-bus"), 0.18f, summaryText = "Bus rules")),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "swimming schedule",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertTrue(ranked.hits.isEmpty())
        assertEquals(RecallPrecision.Exact, ranked.precision)
    }

    /**
     * Zero-overlap paraphrase: `kids water lessons` must still reach a PDF that
     * says `swimming timetable`, and must not pretend those words were found.
     */
    @Test
    fun apply_keeps_a_high_cosine_neighbour_as_meaning_only() = runBlocking {
        val timetable = MemoryRevisionId("rev-timetable")
        val outcome = MeaningSearchOutcome.Matches(
            query = "kids water lessons",
            hits = listOf(
                hit(
                    timetable,
                    0.58f,
                    label = "Grade-2-Swimming-TT-2026.pdf",
                    summaryText = "Grade 2 Swimming Timetable 2026 PERIOD TIME MON TUE",
                ),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "kids water lessons",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(timetable), ranked.hits.map { it.revisionId })
        assertEquals(
            RecallPrecision.MeaningOnly(missing = listOf("kids", "water", "lessons")),
            ranked.precision,
        )
    }

    /**
     * A TIME word is a constraint, not content: the precision gate drops it from
     * the required words while the token boost still rewards a file that happens
     * to contain it. `0.18 + 0.35` then cleared a meaning floor that the cue's
     * real words — `water`, `lessons` — never came close to earning.
     */
    @Test
    fun apply_does_not_admit_a_neighbour_boosted_only_by_a_time_word() = runBlocking {
        val outcome = MeaningSearchOutcome.Matches(
            query = "recent water lessons",
            hits = listOf(
                hit(
                    MemoryRevisionId("rev-bus"),
                    0.18f,
                    label = "bus-rules.pdf",
                    summaryText = "Recent bus rules for parents",
                ),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "recent water lessons",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertTrue(ranked.hits.isEmpty())
        assertEquals(RecallPrecision.Exact, ranked.precision)
    }

    /**
     * Same shape, strong neighbour: it is admitted, but the assist it picked up
     * from the TIME word must not survive — nothing the person named is in that
     * file, so Why cannot go on to say a typed word helped find it.
     */
    @Test
    fun apply_strips_the_time_word_assist_from_an_admitted_meaning_only_hit() = runBlocking {
        val timetable = MemoryRevisionId("rev-timetable")
        val outcome = MeaningSearchOutcome.Matches(
            query = "recent kids water lessons",
            hits = listOf(
                hit(
                    timetable,
                    0.58f,
                    label = "Grade-2-Swimming-TT-2026.pdf",
                    summaryText = "Grade 2 Swimming Timetable 2026, recent revision",
                ),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "recent kids water lessons",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(
            RecallPrecision.MeaningOnly(missing = listOf("kids", "water", "lessons")),
            ranked.precision,
        )
        assertFalse(ranked.hits.single().evidenceTokenBoosted)
        assertEquals(0.58f, ranked.hits.single().score, 0.0001f)
    }

    /** A one-word miss is "this word is not in any file", not a paraphrase. */
    @Test
    fun apply_one_named_word_with_zero_overlap_stays_empty() = runBlocking {
        val outcome = MeaningSearchOutcome.Matches(
            query = "silky",
            hits = listOf(hit(MemoryRevisionId("rev-fashion"), 0.91f, summaryText = "spring catalogue")),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "silky",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertTrue(ranked.hits.isEmpty())
        assertEquals(RecallPrecision.Exact, ranked.precision)
    }

    /**
     * `pool timetable` against a file that says `swimming timetable` is Partial,
     * not MeaningOnly — one named word hit is still a lexical tier.
     */
    @Test
    fun apply_pool_timetable_against_swimming_timetable_is_partial() = runBlocking {
        val timetable = MemoryRevisionId("rev-tt")
        val outcome = MeaningSearchOutcome.Matches(
            query = "pool timetable",
            hits = listOf(
                hit(
                    timetable,
                    0.4f,
                    label = "Grade-2-Swimming-TT-2026.pdf",
                    summaryText = "Grade 2 Swimming Timetable 2026 PERIOD TIME",
                ),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "pool timetable",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(timetable), ranked.hits.map { it.revisionId })
        assertEquals(
            RecallPrecision.Partial(matched = listOf("timetable"), missing = listOf("pool")),
            ranked.precision,
        )
    }

    /**
     * D-14: a higher-scoring picture of UNFYND must sit after the original
     * file, and must not be dropped by later trim just because we reordered.
     */
    @Test
    fun apply_demotes_a_picture_of_unfynd_below_the_original() = runBlocking {
        val original = MemoryRevisionId("rev-pdf")
        val selfie = MemoryRevisionId("rev-selfie")
        val outcome = MeaningSearchOutcome.Matches(
            query = "swimming schedule",
            hits = listOf(
                hit(
                    selfie,
                    0.95f,
                    label = "Screenshot_20260904_124145_UNFYND.png",
                    summaryText = "What are you trying to remember? files have swimming timetable " +
                        "Search by meaning on this phone PDF memory",
                ),
                hit(
                    original,
                    0.55f,
                    label = "Grade-2-Swimming-TT-2026.pdf",
                    summaryText = "Grade 2 Swimming Timetable 2026 PERIOD TIME",
                ),
            ),
            limitReached = false,
            model = model,
        )

        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = "swimming schedule",
            memoryRepository = FakeAnchorRepository(),
        ) as MeaningSearchOutcome.Matches

        assertEquals(listOf(original, selfie), ranked.hits.map { it.revisionId })
    }

    private fun hit(
        revisionId: MemoryRevisionId,
        score: Float,
        label: String = "Label",
        summaryText: String = "Summary",
        rankedPdfPageNumber: Int? = null,
        precisionText: String = "",
    ) = MeaningSearchHit(
        revisionId = revisionId,
        memoryId = MemoryId("mem-${revisionId.value}"),
        sourceId = SourceId("src-1"),
        sourceAssetKey = SourceAssetKey(label),
        assetType = AssetType.PDF,
        label = label,
        summaryText = summaryText,
        score = score,
        model = model,
        rankedPdfPageNumber = rankedPdfPageNumber,
        precisionText = precisionText,
    )

    private fun hit(revisionId: MemoryRevisionId, score: Float) = hit(
        revisionId = revisionId,
        score = score,
        label = "Label",
        summaryText = "Summary",
    )

    private fun topicAnchor(text: String, evidenceId: MemoryEvidenceId) = MemoryAnchor(
        id = MemoryAnchorId("topic-${text.hashCode()}"),
        kind = MemoryAnchorKind.TOPIC,
        text = MemoryText(text),
        evidenceIds = setOf(evidenceId),
    )

    private fun timeAnchor(text: String) = MemoryAnchor(
        id = MemoryAnchorId("time-1"),
        kind = MemoryAnchorKind.TIME,
        text = MemoryText(text),
        evidenceIds = setOf(MemoryEvidenceId("e-time")),
    )

    private class FakeAnchorRepository(
        private val anchors: Map<MemoryRevisionId, List<MemoryAnchor>> = emptyMap(),
    ) : MemoryRepository by EmptyMemoryRepositoryDelegate() {
        /** Counted so T11 can prove the anchor round-trip is not paid per search. */
        var signatureAnchorLookups: Int = 0
            private set

        override suspend fun findSignatureAnchors(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, List<MemoryAnchor>> {
            signatureAnchorLookups++
            return anchors.filterKeys { it in revisionIds }
        }
    }
}
