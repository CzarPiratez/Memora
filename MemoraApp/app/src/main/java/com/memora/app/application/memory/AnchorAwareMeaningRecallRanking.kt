package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningEvidenceTokenBoost
import com.memora.app.domain.memory.AnchorRecallCandidate
import com.memora.app.domain.memory.AnchorStructuredRecallFilter
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.RecallConstraintStrength
import com.memora.app.domain.memory.RecallQueryConstraintClassifier
import com.memora.app.domain.memory.RecallQueryConstraints

/**
 * Shared meaning ranking stage inside [CanonicalRecall] (MIG-07B).
 *
 * Applies disclosed evidence-token boost (L7, moved from candidate gen in Slice 4)
 * then lexical AND filter when the cue carries multiple content tokens (F-02),
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
        val lexicalFiltered = applyLexicalAndFilter(boosted, rawQuery, constraints)
        if (constraints.time == RecallConstraintStrength.NONE &&
            constraints.topic == RecallConstraintStrength.NONE
        ) {
            return lexicalFiltered
        }

        val revisionIds = lexicalFiltered.hits.map(MeaningSearchHit::revisionId).distinct()
        val anchorsByRevision = memoryRepository.findSignatureAnchors(revisionIds)
        val candidates = lexicalFiltered.hits.map { hit ->
            AnchorRecallCandidate(
                revisionId = hit.revisionId,
                baseScore = hit.score,
                anchors = anchorsByRevision[hit.revisionId].orEmpty(),
            )
        }
        val filtered = AnchorStructuredRecallFilter.apply(candidates, constraints)
        val scoreByRevision = filtered.associate { it.revisionId to it.baseScore }
        val reordered = lexicalFiltered.hits
            .filter { scoreByRevision.containsKey(it.revisionId) }
            .sortedByDescending { scoreByRevision[it.revisionId] ?: 0f }
            .map { hit ->
                hit.copy(score = scoreByRevision[hit.revisionId] ?: hit.score)
            }
        return lexicalFiltered.copy(hits = reordered)
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
