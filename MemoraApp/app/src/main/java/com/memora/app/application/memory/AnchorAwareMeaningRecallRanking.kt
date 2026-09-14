package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.domain.intelligence.IdentityRecallRanker
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningEvidenceTokenBoost
import com.memora.app.domain.intelligence.MeaningRecallCue
import com.memora.app.domain.intelligence.RecallPrecision
import com.memora.app.domain.intelligence.UnfyndSelfCapture
import com.memora.app.domain.intelligence.RecallRankCandidate
import com.memora.app.domain.intelligence.RecallRankResult
import com.memora.app.domain.intelligence.RecallRanker
import com.memora.app.domain.memory.AnchorRecallCandidate
import com.memora.app.domain.memory.AnchorStructuredRecallFilter
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.RecallConstraintStrength
import com.memora.app.domain.memory.RecallQueryConstraintClassifier

/**
 * Shared meaning ranking stage inside [CanonicalRecall] (MIG-07B + FC-02 Stage A).
 *
 * Order: token boost → lexical AND filter → [RecallRanker] (Stage A CE or identity) →
 * anchor structured-filter when the query carries TIME/TOPIC cues.
 */
object AnchorAwareMeaningRecallRanking {
    suspend fun apply(
        outcome: MeaningSearchOutcome,
        rawQuery: String,
        memoryRepository: MemoryRepository,
        recallRanker: RecallRanker = IdentityRecallRanker,
    ): MeaningSearchOutcome {
        if (outcome !is MeaningSearchOutcome.Matches) return outcome

        val boosted = applyTokenBoost(outcome, rawQuery)
        val constraints = RecallQueryConstraintClassifier.classify(rawQuery)
        val lexicalFiltered = applyLexicalPrecisionTier(boosted, rawQuery)
        val reranked = applyRecallRanker(lexicalFiltered, rawQuery, recallRanker)
        if (constraints.time == RecallConstraintStrength.NONE &&
            constraints.topic == RecallConstraintStrength.NONE
        ) {
            return demoteSelfCaptures(reranked)
        }

        val revisionIds = reranked.hits.map(MeaningSearchHit::revisionId).distinct()
        val anchorsByRevision = memoryRepository.findSignatureAnchors(revisionIds)
        // Preserve Stage A / identity order from [reranked]; score-only resort would
        // undo CE when TOPIC/TIME is only ADVISORY and anchors are missing/neutral.
        val ceOrder = reranked.hits.mapIndexed { index, hit -> hit.revisionId to index }.toMap()
        val scoreBeforeAnchors = reranked.hits.associate { it.revisionId to it.score }
        val candidates = reranked.hits.map { hit ->
            AnchorRecallCandidate(
                revisionId = hit.revisionId,
                baseScore = hit.score,
                anchors = anchorsByRevision[hit.revisionId].orEmpty(),
            )
        }
        val filtered = AnchorStructuredRecallFilter.apply(candidates, constraints)
        val scoreByRevision = filtered.associate { it.revisionId to it.baseScore }
        val hitByRevision = reranked.hits.associateBy { it.revisionId }
        val reordered = filtered
            .mapNotNull { candidate ->
                val hit = hitByRevision[candidate.revisionId] ?: return@mapNotNull null
                hit.copy(score = candidate.baseScore)
            }
            .sortedWith(
                compareByDescending<MeaningSearchHit> { hit ->
                    val adjusted = scoreByRevision[hit.revisionId] ?: hit.score
                    val before = scoreBeforeAnchors[hit.revisionId] ?: hit.score
                    adjusted - before
                }.thenBy { hit -> ceOrder[hit.revisionId] ?: Int.MAX_VALUE },
            )
        return demoteSelfCaptures(
            reranked.copy(
                hits = reordered,
                // An anchor filter that empties the list also removes the partial
                // answer the banner was going to describe.
                precision = if (reordered.isEmpty()) RecallPrecision.Exact else reranked.precision,
            ),
        )
    }

    /**
     * D-14: a picture of UNFYND matching the cue must not sit above the
     * original. Order inside each group is unchanged. Runs before the
     * trusted-hit trim so a high-cosine self-capture cannot set the band.
     */
    private fun demoteSelfCaptures(
        outcome: MeaningSearchOutcome.Matches,
    ): MeaningSearchOutcome.Matches {
        val originals = outcome.hits.filterNot { hit ->
            UnfyndSelfCapture.matches(hit.label, hit.lexicalHaystack())
        }
        val self = outcome.hits.filter { hit ->
            UnfyndSelfCapture.matches(hit.label, hit.lexicalHaystack())
        }
        if (self.isEmpty() || originals.isEmpty()) return outcome
        return outcome.copy(hits = originals + self)
    }

    private fun applyRecallRanker(
        outcome: MeaningSearchOutcome.Matches,
        rawQuery: String,
        recallRanker: RecallRanker,
    ): MeaningSearchOutcome.Matches {
        if (outcome.hits.isEmpty()) return outcome
        val candidates = outcome.hits.map { hit ->
            RecallRankCandidate(
                id = hit.revisionId.value,
                score = hit.score,
                excerpt = hit.summaryText,
            )
        }
        return when (val ranked = recallRanker.rank(rawQuery, candidates)) {
            is RecallRankResult.Unavailable -> outcome
            is RecallRankResult.Ranked -> {
                val byId = outcome.hits.associateBy { it.revisionId.value }
                val ordered = ranked.orderedIds.mapNotNull { byId[it] }
                if (ordered.size != outcome.hits.size) return outcome
                outcome.copy(hits = ordered)
            }
        }
    }

    /**
     * Lexical precision as a **tier**, not a veto (defect D-12).
     *
     * Hits whose stored text carries every named word win outright — that is the
     * unchanged exact behaviour. When no hit carries all of them, the list falls
     * to the best tier available and reports what it could not match, rather than
     * answering an empty screen: `swimming schedule` must still reach a PDF that
     * says `swimming timetable`, and must say plainly that nothing contained
     * `schedule`. UNFYND does not claim the two words mean the same thing.
     *
     * When no named word appears anywhere, a two-or-more-word cue may still keep
     * a short high-cosine band as [RecallPrecision.MeaningOnly] — the remaining
     * hole after D-12/D-15. A one-word miss stays empty. This is not a synonym
     * net: the banner must say the words were not found. That band is judged on
     * raw similarity, so the boost applied above cannot buy a seat here; see
     * [MeaningOnlyRecallPolicy].
     *
     * A time cue must not disable precision: `recent files with silky` still has
     * to contain `silky`, while `notes in 2024` requires `notes` and leaves
     * `2024` to the TIME anchor stage (bar T10). [MeaningRecallCue.contentTokens]
     * makes that the same list the query vector was built from (defect D-10).
     */
    private fun applyLexicalPrecisionTier(
        outcome: MeaningSearchOutcome.Matches,
        rawQuery: String,
    ): MeaningSearchOutcome.Matches {
        val required = MeaningRecallCue.contentTokens(rawQuery)
        if (required.isEmpty() || outcome.hits.isEmpty()) return outcome

        val cue = MeaningEvidenceLexicalFilter.prepare(required)
        val scored = outcome.hits.map { hit ->
            hit to cue.matchingTokens(hit.lexicalHaystack())
        }

        val exact = scored.filter { it.second.size == required.size }.map { it.first }
        if (exact.isNotEmpty()) return outcome.copy(hits = exact)

        // Prefer the tier that satisfies most of the words the person used, then
        // let the highest-ranked hit at that depth define which words those are,
        // so one banner can describe the whole list truthfully.
        val deepest = scored.maxOf { it.second.size }
        if (deepest == 0) {
            val admitted = MeaningOnlyRecallPolicy.select(outcome.hits, required.size)
            return if (admitted.isEmpty()) {
                outcome.copy(hits = emptyList(), precision = RecallPrecision.Exact)
            } else {
                outcome.copy(
                    hits = admitted,
                    precision = RecallPrecision.MeaningOnly(missing = required),
                )
            }
        }
        val matched = scored.first { it.second.size == deepest }.second
        return outcome.copy(
            hits = scored.filter { it.second == matched }.map { it.first },
            precision = RecallPrecision.Partial(
                matched = matched,
                missing = required - matched.toSet(),
            ),
        )
    }

    private fun applyTokenBoost(
        outcome: MeaningSearchOutcome.Matches,
        rawQuery: String,
    ): MeaningSearchOutcome.Matches {
        val boostedHits = outcome.hits.map { hit ->
            val (score, tokenBoosted) = MeaningEvidenceTokenBoost.apply(
                cosine = hit.score,
                query = rawQuery,
                evidenceText = hit.lexicalHaystack(),
            )
            hit.copy(score = score, evidenceTokenBoosted = tokenBoosted)
        }.sortedByDescending { it.score }
        return outcome.copy(hits = boostedHits)
    }
}
