package com.memora.app.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorStructuredRecallFilterTest {
    private val revisionA = MemoryRevisionId("rev-a")
    private val revisionB = MemoryRevisionId("rev-b")
    private val revisionC = MemoryRevisionId("rev-c")

    @Test
    fun explicit_time_excludes_only_candidates_with_non_matching_time_anchor() {
        val constraints = RecallQueryConstraints(
            time = RecallConstraintStrength.EXPLICIT,
            topic = RecallConstraintStrength.NONE,
            timeCue = "2024",
        )
        val candidates = listOf(
            candidate(revisionA, 0.9f, timeText = "Date taken: 2024-03-01"),
            candidate(revisionB, 0.8f, timeText = "Date taken: 2022-01-01"),
            candidate(revisionC, 0.7f, timeText = null),
        )

        val result = AnchorStructuredRecallFilter.apply(candidates, constraints)

        assertEquals(listOf(revisionA, revisionC), result.map { it.revisionId })
    }

    @Test
    fun advisory_time_boosts_matches_without_excluding_missing_anchor() {
        val constraints = RecallQueryConstraints(
            time = RecallConstraintStrength.ADVISORY,
            topic = RecallConstraintStrength.NONE,
            timeCue = "recent",
        )
        val candidates = listOf(
            candidate(revisionA, 0.5f, timeText = "recent trip notes"),
            candidate(revisionB, 0.6f, timeText = null),
        )

        val result = AnchorStructuredRecallFilter.apply(candidates, constraints)

        assertEquals(2, result.size)
        assertTrue(result.first().revisionId == revisionA)
        assertTrue(result.first().baseScore > 0.5f)
        assertEquals(0.6f, result[1].baseScore, 0.0001f)
    }

    @Test
    fun explicit_topic_excludes_non_matching_topic_anchor() {
        val constraints = RecallQueryConstraints(
            time = RecallConstraintStrength.NONE,
            topic = RecallConstraintStrength.EXPLICIT,
            topicCue = "invoice",
        )
        val candidates = listOf(
            candidate(revisionA, 0.9f, topicText = "invoice march"),
            candidate(revisionB, 0.85f, topicText = "holiday photos"),
            candidate(revisionC, 0.8f, topicText = null),
        )

        val result = AnchorStructuredRecallFilter.apply(candidates, constraints)

        assertEquals(listOf(revisionA, revisionC), result.map { it.revisionId })
    }

    @Test
    fun new_topic_kind_does_not_change_query_without_topic_cue() {
        val constraints = RecallQueryConstraintClassifier.classify("wifi password")
        val before = listOf(
            candidate(revisionA, 0.4f, topicText = "invoice"),
            candidate(revisionB, 0.5f, topicText = null),
        )
        val after = listOf(
            candidate(revisionA, 0.4f, topicText = "invoice"),
            candidate(revisionB, 0.5f, topicText = null, timeText = "2024-01-01"),
        )

        val beforeResult = AnchorStructuredRecallFilter.apply(before, constraints)
        val afterResult = AnchorStructuredRecallFilter.apply(after, constraints)

        assertEquals(beforeResult.map { it.revisionId }, afterResult.map { it.revisionId })
    }

    private fun candidate(
        revisionId: MemoryRevisionId,
        score: Float,
        timeText: String? = null,
        topicText: String? = null,
    ): AnchorRecallCandidate {
        val anchors = buildList {
            add(
                MemoryAnchor(
                    id = MemoryAnchorId("text-1"),
                    kind = MemoryAnchorKind.TEXT,
                    text = MemoryText("body"),
                    evidenceIds = setOf(MemoryEvidenceId("e1")),
                ),
            )
            timeText?.let {
                add(
                    MemoryAnchor(
                        id = MemoryAnchorId("time-1"),
                        kind = MemoryAnchorKind.TIME,
                        text = MemoryText(it),
                        evidenceIds = setOf(MemoryEvidenceId("e-time")),
                    ),
                )
            }
            topicText?.let {
                add(
                    MemoryAnchor(
                        id = MemoryAnchorId("topic-1"),
                        kind = MemoryAnchorKind.TOPIC,
                        text = MemoryText(it),
                        evidenceIds = setOf(MemoryEvidenceId("e-topic")),
                    ),
                )
            }
        }
        return AnchorRecallCandidate(
            revisionId = revisionId,
            baseScore = score,
            anchors = anchors,
        )
    }
}
