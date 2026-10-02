package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.domain.intelligence.MeaningRecallCue
import com.memora.app.domain.intelligence.ModelVersionIdentity

/**
 * Union of meaning cosine candidates and lexical evidence hits inside
 * Canonical Recall (ADR-055 slice 2).
 *
 * An asset already in the meaning pool keeps its measured cosine.
 * A lexical-only asset is appended at [LEXICAL_ONLY_COSINE] so it cannot
 * set the trusted band. Role score still ranks the fused list.
 */
object MeaningCandidateFusion {
    const val LEXICAL_ONLY_COSINE = MeaningRecallCue.MIN_CANDIDATE_SCORE

    fun merge(
        meaning: MeaningSearchOutcome.Matches,
        lexicalHits: List<MemoryEvidenceSearchHit>,
    ): MeaningSearchOutcome.Matches {
        if (lexicalHits.isEmpty()) return meaning
        val seen = meaning.hits.map {
            ReciprocalRankFusion.fusionKey(it.sourceId, it.sourceAssetKey)
        }.toHashSet()
        val extras = lexicalHits.mapNotNull { hit ->
            val key = ReciprocalRankFusion.fusionKey(hit.sourceId, hit.sourceAssetKey)
            if (key in seen) null else asMeaningHit(hit, meaning.model)
        }
        if (extras.isEmpty()) return meaning
        return meaning.copy(hits = meaning.hits + extras)
    }

    private fun asMeaningHit(
        hit: MemoryEvidenceSearchHit,
        model: ModelVersionIdentity,
    ): MeaningSearchHit = MeaningSearchHit(
        revisionId = hit.revisionId,
        memoryId = hit.memoryId,
        sourceId = hit.sourceId,
        sourceAssetKey = hit.sourceAssetKey,
        assetType = hit.assetType,
        label = hit.label,
        summaryText = hit.excerpt,
        score = LEXICAL_ONLY_COSINE,
        model = model,
        rankedPdfPageNumber = hit.openPageNumber,
        precisionText = "${hit.excerpt} ${hit.label} ${hit.sourceAssetKey.value}",
        cosine = LEXICAL_ONLY_COSINE,
    )
}
