package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.domain.intelligence.MeaningEvidenceTokenBoost
import com.memora.app.domain.memory.AnchorRecallCandidate
import com.memora.app.domain.memory.AnchorStructuredRecallFilter
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.RecallConstraintStrength
import com.memora.app.domain.memory.RecallQueryConstraintClassifier

/**
 * Shared meaning ranking stage inside [CanonicalRecall] (MIG-07B).
 *
 * Applies disclosed evidence-token boost (L7, moved from candidate gen in Slice 4)
 * then anchor structured-filter when the query carries TIME/TOPIC cues. Missing
 * anchors remain neutral per Freeze §3.
 */
object AnchorAwareMeaningRecallRanking {
    suspend fun apply(
        outcome: MeaningSearchOutcome,
        rawQuery: String,
        memoryRepository: MemoryRepository,
    ): MeaningSearchOutcome {
        if (outcome !is MeaningSearchOutcome.Matches) return outcome

        val boosted = applyTokenBoost(outcome, rawQuery)
        val constraints = RecallQueryConstraintClassifier.classify(rawQuery)
        if (constraints.time == RecallConstraintStrength.NONE &&
            constraints.topic == RecallConstraintStrength.NONE
        ) {
            return boosted
        }

        val revisionIds = boosted.hits.map(MeaningSearchHit::revisionId).distinct()
        val anchorsByRevision = memoryRepository.findSignatureAnchors(revisionIds)
        val candidates = boosted.hits.map { hit ->
            AnchorRecallCandidate(
                revisionId = hit.revisionId,
                baseScore = hit.score,
                anchors = anchorsByRevision[hit.revisionId].orEmpty(),
            )
        }
        val filtered = AnchorStructuredRecallFilter.apply(candidates, constraints)
        val scoreByRevision = filtered.associate { it.revisionId to it.baseScore }
        val reordered = boosted.hits
            .filter { scoreByRevision.containsKey(it.revisionId) }
            .sortedByDescending { scoreByRevision[it.revisionId] ?: 0f }
            .map { hit ->
                hit.copy(score = scoreByRevision[hit.revisionId] ?: hit.score)
            }
        return boosted.copy(hits = reordered)
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
