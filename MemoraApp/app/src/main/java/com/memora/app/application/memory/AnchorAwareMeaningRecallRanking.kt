package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.domain.intelligence.IdentityRecallRanker
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningEvidenceTokenBoost
import com.memora.app.domain.intelligence.RecallRankCandidate
import com.memora.app.domain.intelligence.RecallRankResult
import com.memora.app.domain.intelligence.RecallRanker
import com.memora.app.domain.memory.AnchorRecallCandidate
import com.memora.app.domain.memory.AnchorStructuredRecallFilter
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.RecallConstraintStrength
import com.memora.app.domain.memory.RecallQueryConstraintClassifier
import com.memora.app.domain.memory.RecallQueryConstraints

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
        val lexicalFiltered = applyLexicalAndFilter(boosted, rawQuery, constraints)
        val reranked = applyRecallRanker(lexicalFiltered, rawQuery, recallRanker)
        if (constraints.time == RecallConstraintStrength.NONE &&
            constraints.topic == RecallConstraintStrength.NONE
        ) {
            return reranked
        }

        val revisionIds = reranked.hits.map(MeaningSearchHit::revisionId).distinct()
        val anchorsByRevision = memoryRepository.findSignatureAnchors(revisionIds)
        val candidates = reranked.hits.map { hit ->
            AnchorRecallCandidate(
                revisionId = hit.revisionId,
                baseScore = hit.score,
                anchors = anchorsByRevision[hit.revisionId].orEmpty(),
            )
        }
        val filtered = AnchorStructuredRecallFilter.apply(candidates, constraints)
        val scoreByRevision = filtered.associate { it.revisionId to it.baseScore }
        val reordered = reranked.hits
            .filter { scoreByRevision.containsKey(it.revisionId) }
            .sortedByDescending { scoreByRevision[it.revisionId] ?: 0f }
            .map { hit ->
                hit.copy(score = scoreByRevision[hit.revisionId] ?: hit.score)
            }
        return reranked.copy(hits = reordered)
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

    private fun applyLexicalAndFilter(
        outcome: MeaningSearchOutcome.Matches,
        rawQuery: String,
        constraints: RecallQueryConstraints,
    ): MeaningSearchOutcome.Matches {
        if (constraints.time != RecallConstraintStrength.NONE) return outcome
        if (!MeaningEvidenceLexicalFilter.shouldApply(rawQuery)) return outcome
        val filtered = outcome.hits.filter { hit ->
            MeaningEvidenceLexicalFilter.evidenceSatisfies(rawQuery, hit.summaryText)
        }
        return outcome.copy(hits = filtered)
    }

    private fun applyTokenBoost(
        outcome: MeaningSearchOutcome.Matches,
        rawQuery: String,
    ): MeaningSearchOutcome.Matches {
        val boostedHits = outcome.hits.map { hit ->
            val (score, tokenBoosted) = MeaningEvidenceTokenBoost.apply(
                cosine = hit.score,
                query = rawQuery,
                evidenceText = hit.summaryText,
            )
            hit.copy(score = score, evidenceTokenBoosted = tokenBoosted)
        }.sortedByDescending { it.score }
        return outcome.copy(hits = boostedHits)
    }
}
