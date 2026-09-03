package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BertWordPieceTokenizerTest {
    @Test
    fun matches_huggingface_golden_ids_for_mira_fixture_pair() {
        val tokenizer = loadVocab()
        val encoded = tokenizer.encodePair(
            query = "mira",
            passage = "Page 5 JULIET meet mira closing",
            maxLength = 32,
        )
        // Generated 2026-09-03 via transformers AutoTokenizer on
        // temsa/ms-marco-MiniLM-L-6-v2-onnx-cpu-qint8
        val expectedIds = longArrayOf(
            101, 18062, 102, 3931, 1019, 13707, 3113, 18062, 5494, 102,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        )
        val expectedMask = longArrayOf(
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        )
        val expectedTypes = longArrayOf(
            0, 0, 0, 1, 1, 1, 1, 1, 1, 1,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        )
        assertTrue(expectedIds.contentEquals(encoded.inputIds))
        assertTrue(expectedMask.contentEquals(encoded.attentionMask))
        assertTrue(expectedTypes.contentEquals(encoded.tokenTypeIds))
    }

    @Test
    fun matches_huggingface_golden_ids_for_invoice_fixture_pair() {
        val tokenizer = loadVocab()
        val encoded = tokenizer.encodePair(
            query = "invoice",
            passage = "Page 2 GOLF invoice number",
            maxLength = 32,
        )
        val expectedIds = longArrayOf(
            101, 1999, 6767, 6610, 102, 3931, 1016, 5439, 1999, 6767, 6610, 2193, 102,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        )
        assertTrue(expectedIds.contentEquals(encoded.inputIds))
    }

    @Test
    fun loads_vocab_from_test_resources_and_encodes_pair_shape() {
        val tokenizer = loadVocab()
        val encoded = tokenizer.encodePair(
            query = "mira",
            passage = "Page 5 JULIET meet mira closing",
            maxLength = 32,
        )
        assertEquals(32, encoded.length)
        assertEquals(101L, encoded.inputIds[0]) // [CLS]
        assertTrue(encoded.attentionMask.count { it == 1L } >= 5)
        assertTrue(encoded.tokenTypeIds.any { it == 1L })
        assertEquals(0L, encoded.tokenTypeIds[0])
        if (encoded.inputIds.last() == 0L) {
            assertEquals(0L, encoded.attentionMask.last())
        }
    }

    @Test
    fun encodes_fixture_cues_without_blank_ids_before_sep() {
        val tokenizer = loadVocab()
        MeaningPdfPageRecallCorpus.labeledCases().forEach { labeled ->
            labeled.candidates.forEach { candidate ->
                val encoded = tokenizer.encodePair(
                    labeled.cue,
                    candidate.evidenceText,
                    maxLength = 64,
                )
                assertEquals(64, encoded.length)
                assertEquals(101L, encoded.inputIds[0])
                assertTrue(encoded.inputIds.contains(102L))
            }
        }
    }

    private fun loadVocab(): BertWordPieceTokenizer {
        val stream = checkNotNull(
            javaClass.classLoader?.getResourceAsStream("recall_rank/bert_vocab.txt"),
        ) { "Missing test resource recall_rank/bert_vocab.txt" }
        return stream.bufferedReader().use { BertWordPieceTokenizer.loadFromReader(it) }
    }
}

class ScoreMeaningPdfPageRecallWithCrossEncoderTest {
    @Test
    fun meets_s2_bar_on_stage_a_cases_when_scorer_prefers_labeled_pages() {
        val scorer = CrossEncoderPairScorer { query, passage ->
            when {
                query == "mira" && passage.contains("JULIET meet mira") -> 3f
                query == "boarding" && passage.contains("boarding pass") -> 3f
                query == "invoice" && passage.contains("invoice number") -> 3f
                else -> 0f
            }
        }
        val report = ScoreMeaningPdfPageRecallWithCrossEncoder.score(
            scorer = scorer,
            labeledCases = MeaningPdfPageRecallCorpus.stageALabeledCases(),
        )
        assertEquals(3, report.caseCount)
        assertEquals(3, report.crossEncoderHitsAt1)
        assertTrue(report.meetsS2Bar)
    }

    @Test
    fun meets_s2_bar_when_scorer_prefers_labeled_pages() {
        val scorer = CrossEncoderPairScorer { query, passage ->
            when {
                query == "mira" && passage.contains("JULIET meet mira") -> 3f
                query == "boarding" && passage.contains("boarding pass") -> 3f
                query == "invoice" && passage.contains("invoice number") -> 3f
                else -> 0f
            }
        }
        val report = ScoreMeaningPdfPageRecallWithCrossEncoder.score(scorer)
        assertEquals(3, report.caseCount)
        assertEquals(3, report.crossEncoderHitsAt1)
        assertEquals(3, report.caseBreakdown.size)
        assertTrue(report.caseBreakdown.all { it.hitAt1 })
        assertTrue(report.meetsS2Bar)
        assertTrue(report.pairScoreCount >= 9)
    }

    @Test
    fun fails_s2_bar_when_scorer_ranks_distractors_first() {
        val scorer = CrossEncoderPairScorer { _, passage ->
            if (passage.contains("cover sheet") || passage.contains("receipt")) 5f else 0f
        }
        val report = ScoreMeaningPdfPageRecallWithCrossEncoder.score(scorer)
        assertTrue(report.crossEncoderHitsAt1 < report.caseCount)
        assertFalse(report.meetsS2Bar)
    }
}
