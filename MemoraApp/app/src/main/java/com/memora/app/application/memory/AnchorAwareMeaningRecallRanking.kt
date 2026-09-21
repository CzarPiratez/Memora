package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.application.intelligence.MeaningSearchTrace
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
 * Order: token boost → lexical precision tier → [RecallRanker] (Stage A CE or identity) →
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
     * Lexical precision as a **tier**, not a veto (defects D-12 and D-20).
     *
     * Coverage still describes the list and still boosts a hit, but it is not a
     * subset: Exact must not delete Partial (D-20), and one named-word family
     * must not delete another that is already in the admitted pool (D-21).
     * UNFYND does not claim `schedule` means `timetable`.
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

        val deepest = scored.maxOf { it.second.size }
        if (deepest == 0) {
            val admitted = MeaningOnlyRecallPolicy.select(outcome.hits, required.size)
            return if (admitted.isEmpty()) {
                outcome.copy(
                    hits = emptyList(),
                    precision = RecallPrecision.Exact,
                    debugTrace = MeaningSearchTrace.withTierDrops(
                        current = outcome.debugTrace,
                        before = outcome.hits,
                        after = emptyList(),
                    ),
                )
            } else {
                outcome.copy(
                    hits = admitted,
                    precision = RecallPrecision.MeaningOnly(missing = required),
                    debugTrace = MeaningSearchTrace.withTierDrops(
                        current = outcome.debugTrace,
                        before = outcome.hits,
                        after = admitted,
                    ),
                )
            }
        }

        // D-20 / D-21: keep every hit that carries at least one named word.
        // Coverage is a boost and a banner, not a subset — whether or not an
        // Exact file exists. Zero-overlap neighbours still drop.
        val hits = scored.filter { it.second.isNotEmpty() }.map { it.first }
        return outcome.copy(
            hits = hits,
            precision = precisionOf(hits, required, cue),
            debugTrace = MeaningSearchTrace.withTierDrops(
                current = outcome.debugTrace,
                before = outcome.hits,
                after = hits,
            ),
        )
    }

    /**
     * Trusted-hit trim can drop Exact rows and leave mixed Partial families.
     * Re-describe the remaining page. Do not subset back to one word family
     * (D-21): a cosine band must not undo named-word keep.
     */
    internal fun refineAfterTrustedTrim(
        hits: List<MeaningSearchHit>,
        rawQuery: String,
    ): Pair<List<MeaningSearchHit>, RecallPrecision> {
        if (hits.isEmpty()) return hits to RecallPrecision.Exact
        val required = MeaningRecallCue.contentTokens(rawQuery)
        if (required.isEmpty()) return hits to RecallPrecision.Exact

        val cue = MeaningEvidenceLexicalFilter.prepare(required)
        val scored = hits.map { hit ->
            hit to cue.matchingTokens(hit.lexicalHaystack())
        }
        val deepest = scored.maxOf { it.second.size }
        if (deepest == 0) {
            return if (required.size >= RecallPrecision.MeaningOnly.MIN_NAMED_WORDS) {
                hits to RecallPrecision.MeaningOnly(missing = required)
            } else {
                emptyList<MeaningSearchHit>() to RecallPrecision.Exact
            }
        }
        val retained = scored.filter { it.second.isNotEmpty() }.map { it.first }
        return retained to precisionOf(retained, required, cue)
    }

    private fun precisionOf(
        hits: List<MeaningSearchHit>,
        required: List<String>,
        cue: MeaningEvidenceLexicalFilter.PreparedCue,
    ): RecallPrecision {
        if (hits.isEmpty() || required.isEmpty()) return RecallPrecision.Exact
        val tokenLists = hits.map { cue.matchingTokens(it.lexicalHaystack()) }
        val deepest = tokenLists.maxOf { it.size }
        if (deepest == 0) {
            return if (required.size >= RecallPrecision.MeaningOnly.MIN_NAMED_WORDS) {
                RecallPrecision.MeaningOnly(missing = required)
            } else {
                RecallPrecision.Exact
            }
        }
        if (tokenLists.all { it.size == required.size }) return RecallPrecision.Exact
        val tokenSets = tokenLists.map { it.toSet() }
        val intersection = tokenSets.reduce { a, b -> a intersect b }
        val union = tokenSets.reduce { a, b -> a union b }
        val missingFromAll = required.filter { it !in union }
        val missing = missingFromAll.ifEmpty {
            required.filter { it !in intersection }
        }
        val exactHitsPresent = tokenLists.any { it.size == required.size }
        val mixedNamedWordFamilies = missingFromAll.isEmpty() && !exactHitsPresent
        val matched = when {
            exactHitsPresent ->
                required.filter { it in intersection }.ifEmpty {
                    required.filter { it in union }
                }
            else -> required.filter { it in union }
        }
        if (matched.isEmpty() || missing.isEmpty()) return RecallPrecision.Exact
        return RecallPrecision.Partial(
            matched = matched,
            missing = missing,
            exactHitsPresent = exactHitsPresent,
            mixedNamedWordFamilies = mixedNamedWordFamilies,
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
