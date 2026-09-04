package com.memora.app.domain.memory

/**
 * How strongly a recall cue constrains TIME or TOPIC anchors (MIG-07B / Freeze §3).
 *
 * [NONE] — query carries no cue for this dimension.
 * [ADVISORY] — cue may boost matching candidates; never excludes on missing anchor.
 * [EXPLICIT] — satisfying anchor required when present on candidate; missing anchor
 *   on candidate remains neutral (never exclusionary).
 */
enum class RecallConstraintStrength {
    NONE,
    ADVISORY,
    EXPLICIT,
}

/** Parsed constraint interpretation for one recall query. */
data class RecallQueryConstraints(
    val time: RecallConstraintStrength,
    val topic: RecallConstraintStrength,
    /** Normalized substring cue for TIME matching, when [time] != [NONE]. */
    val timeCue: String? = null,
    /** Normalized substring cue for TOPIC matching, when [topic] != [NONE]. */
    val topicCue: String? = null,
    /**
     * The literal time expression matched in the query (`in 2024`, `recent`,
     * `last week`). Those words are a **constraint**, not content: the lexical
     * precision gate must not demand they appear in stored text, while every
     * other named word still must (bar T10).
     */
    val timeSpanText: String? = null,
) {
    init {
        require(timeCue == null || time != RecallConstraintStrength.NONE) {
            "timeCue requires a non-NONE time strength."
        }
        require(topicCue == null || topic != RecallConstraintStrength.NONE) {
            "topicCue requires a non-NONE topic strength."
        }
        require(timeSpanText == null || time != RecallConstraintStrength.NONE) {
            "timeSpanText requires a non-NONE time strength."
        }
    }
}

/**
 * Deterministic, bounded query-constraint classifier (MIG-07B).
 *
 * Not general NLU — resolves explicit vs advisory vs none for TIME/TOPIC only.
 */
object RecallQueryConstraintClassifier {
    private val explicitTimePatterns = listOf(
        Regex("""\b(in|on|during)\s+(\d{4})\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(\d{4}-\d{2}-\d{2})\b"""),
        Regex(
            """\b(january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{1,2}(?:,?\s+\d{4})?\b""",
            RegexOption.IGNORE_CASE,
        ),
    )
    private val advisoryTimePatterns = listOf(
        Regex("""\b(recent|recently|old|older|last\s+week|last\s+month|last\s+year)\b""", RegexOption.IGNORE_CASE),
    )
    private val explicitTopicPatterns = listOf(
        Regex("""\btitled\s+["“](.+?)["”]""", RegexOption.IGNORE_CASE),
        Regex("""\b(title|called)\s+["“](.+?)["”]""", RegexOption.IGNORE_CASE),
    )

    fun classify(rawQuery: String): RecallQueryConstraints {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            return RecallQueryConstraints(
                time = RecallConstraintStrength.NONE,
                topic = RecallConstraintStrength.NONE,
            )
        }

        // Keep the cue (what a TIME anchor must contain) and the full matched span
        // (what the lexical gate must not demand) from the same match.
        val timeExplicit = explicitTimePatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(query)?.let { match ->
                match.groupValues.drop(1).lastOrNull { it.isNotBlank() }?.trim()?.let { cue ->
                    cue to match.value
                }
            }
        }
        if (timeExplicit != null) {
            val (cue, span) = timeExplicit
            val topicStrength = classifyTopic(query)
            return RecallQueryConstraints(
                time = RecallConstraintStrength.EXPLICIT,
                topic = topicStrength,
                timeCue = cue.lowercase(),
                topicCue = topicCueOrNull(query, topicStrength),
                timeSpanText = span,
            )
        }

        val timeAdvisory = advisoryTimePatterns.firstNotNullOfOrNull { it.find(query) }
        val topicStrength = classifyTopic(query)
        return RecallQueryConstraints(
            time = if (timeAdvisory != null) RecallConstraintStrength.ADVISORY else RecallConstraintStrength.NONE,
            topic = topicStrength,
            timeCue = timeAdvisory?.value?.trim()?.lowercase(),
            topicCue = topicCueOrNull(query, topicStrength),
            timeSpanText = timeAdvisory?.value,
        )
    }

    /**
     * TOPIC is a constraint only when the person actually names a title
     * (`titled "March Invoice"`). Treating every query of a few characters as an
     * advisory topic cue made the whole raw question the title to match, which no
     * anchor can ever contain, while still costing an anchor lookup on every
     * search (bar T11). A topic boost that works belongs to a later slice with
     * its own cue extraction and tests.
     */
    private fun classifyTopic(query: String): RecallConstraintStrength =
        if (explicitTopicPatterns.any { it.containsMatchIn(query) }) {
            RecallConstraintStrength.EXPLICIT
        } else {
            RecallConstraintStrength.NONE
        }

    private fun topicCueOrNull(
        query: String,
        strength: RecallConstraintStrength,
    ): String? {
        if (strength == RecallConstraintStrength.NONE) return null
        explicitTopicPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(query)?.groupValues?.drop(1)?.lastOrNull { it.isNotBlank() }
        }?.let { return it.trim().lowercase() }
        return query.trim().lowercase().takeIf { it.isNotBlank() }
    }
}

/** One ranked recall candidate entering the structured-filter stage. */
data class AnchorRecallCandidate(
    val revisionId: MemoryRevisionId,
    val baseScore: Float,
    val anchors: List<MemoryAnchor>,
) {
    init {
        require(baseScore.isFinite())
    }
}

/**
 * Deterministic structured filter over TIME/TOPIC anchors (MIG-07B).
 *
 * Missing anchors on a candidate are always neutral. Explicit constraints exclude
 * only when the candidate has the anchor kind and it does not satisfy the cue.
 */
object AnchorStructuredRecallFilter {
    private const val ADVISORY_BOOST = 0.15f

    fun apply(
        candidates: List<AnchorRecallCandidate>,
        constraints: RecallQueryConstraints,
    ): List<AnchorRecallCandidate> =
        candidates
            .mapNotNull { candidate ->
                if (violatesExplicitConstraint(candidate, constraints)) return@mapNotNull null
                candidate.copy(baseScore = adjustedScore(candidate, constraints))
            }
            .sortedByDescending { it.baseScore }

    private fun violatesExplicitConstraint(
        candidate: AnchorRecallCandidate,
        constraints: RecallQueryConstraints,
    ): Boolean {
        if (constraints.time == RecallConstraintStrength.EXPLICIT) {
            val timeAnchor = candidate.anchors.firstOrNull { it.kind == MemoryAnchorKind.TIME }
            if (timeAnchor != null && !anchorSatisfiesCue(timeAnchor.text.value, constraints.timeCue)) {
                return true
            }
        }
        if (constraints.topic == RecallConstraintStrength.EXPLICIT) {
            val topicAnchor = candidate.anchors.firstOrNull { it.kind == MemoryAnchorKind.TOPIC }
            if (topicAnchor != null && !anchorSatisfiesCue(topicAnchor.text.value, constraints.topicCue)) {
                return true
            }
        }
        return false
    }

    private fun adjustedScore(
        candidate: AnchorRecallCandidate,
        constraints: RecallQueryConstraints,
    ): Float {
        var score = candidate.baseScore
        if (constraints.time == RecallConstraintStrength.ADVISORY) {
            val timeAnchor = candidate.anchors.firstOrNull { it.kind == MemoryAnchorKind.TIME }
            if (timeAnchor != null && anchorSatisfiesCue(timeAnchor.text.value, constraints.timeCue)) {
                score += ADVISORY_BOOST
            }
        }
        if (constraints.topic == RecallConstraintStrength.ADVISORY) {
            val topicAnchor = candidate.anchors.firstOrNull { it.kind == MemoryAnchorKind.TOPIC }
            if (topicAnchor != null && anchorSatisfiesCue(topicAnchor.text.value, constraints.topicCue)) {
                score += ADVISORY_BOOST
            }
        }
        return score
    }

    private fun anchorSatisfiesCue(anchorText: String, cue: String?): Boolean {
        if (cue.isNullOrBlank()) return true
        return anchorText.lowercase().contains(cue)
    }
}
