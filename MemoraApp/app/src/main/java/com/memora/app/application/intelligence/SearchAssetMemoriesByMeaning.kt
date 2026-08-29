package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingSimilarity
import com.memora.app.domain.intelligence.MeaningEvidenceTokenBoost
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Candidate Find-by-meaning over indexed Memory summaries and evidence-level
 * vectors (MIG-05 step 3+; PdfPageEmbedding* retired in step 4).
 *
 * Page/evidence ranking reads [MemoryEvidenceEmbeddingStore] + stored
 * [com.memora.app.domain.memory.MemoryEvidence] excerpts. Does not use
 * [com.memora.app.domain.extraction.SavedPdfPageTextSource] for ranking.
 * Compact-model evidence-token boost may still apply. Does not claim Local
 * Intelligence marketing AVAILABLE / SLA (ADR-024/025).
 */
class SearchAssetMemoriesByMeaning @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val embeddingStore: MemoryEmbeddingStore,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
    private val memoryRepository: MemoryRepository,
    private val applyMig05EvidenceSearchCutover: ApplyMig05EvidenceSearchCutover,
) {
    suspend operator fun invoke(
        rawQuery: String,
        limit: Int = DEFAULT_LIMIT,
    ): MeaningSearchOutcome = withContext(Dispatchers.Default) {
        require(limit > 0)
        val query = rawQuery.trim()
        if (query.isEmpty()) return@withContext MeaningSearchOutcome.BlankQuery

        val model = when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                return@withContext MeaningSearchOutcome.EngineUnavailable(availability.reason)
            is CapabilityAvailability.Available -> availability.model
        }

        applyMig05EvidenceSearchCutover.ensureApplied(model)

        val summaryIndexed = embeddingStore.listForModel(model)
        val evidenceIndexed = evidenceEmbeddingStore.listForModel(model)
        if (summaryIndexed.isEmpty() && evidenceIndexed.isEmpty()) {
            return@withContext MeaningSearchOutcome.NothingIndexed(query = query)
        }

        val queryVector = when (val encoded = embeddingEngine.embedText(query)) {
            is EmbeddingEncodeResult.Unavailable ->
                return@withContext MeaningSearchOutcome.EngineUnavailable(encoded.reason)
            is EmbeddingEncodeResult.Failed ->
                return@withContext MeaningSearchOutcome.Failed(encoded.reason)
            is EmbeddingEncodeResult.Success -> encoded.vector
        }

        val revisionIds = (summaryIndexed.map { it.revisionId } + evidenceIndexed.map { it.revisionId })
            .distinct()
        val lookups = memoryRepository.findCurrentReadyMeaningLookups(revisionIds)
        val evidenceRows = memoryRepository.findEvidenceSearchRows(
            evidenceIndexed.map { it.revisionId }.distinct(),
        )

        val summaryHits = summaryIndexed.mapNotNull { record ->
            val lookup = lookups[record.revisionId] ?: return@mapNotNull null
            if (record.vector.dimensions != queryVector.dimensions) return@mapNotNull null
            val cosine = EmbeddingSimilarity.cosine(queryVector, record.vector)
            val (score, tokenBoosted) = MeaningEvidenceTokenBoost.apply(
                cosine = cosine,
                query = query,
                evidenceText = lookup.summaryText,
            )
            if (score < MIN_CANDIDATE_SCORE) return@mapNotNull null
            MeaningSearchHit(
                revisionId = record.revisionId,
                memoryId = record.memoryId,
                sourceId = lookup.sourceId,
                sourceAssetKey = lookup.sourceAssetKey,
                assetType = lookup.assetType,
                label = lookup.displayLabel,
                summaryText = lookup.summaryText,
                citedPdfPageNumber = lookup.citedPdfPageNumber,
                rankedPdfPageNumber = null,
                score = score,
                model = model,
                evidenceTokenBoosted = tokenBoosted,
            )
        }

        val evidenceHits = evidenceIndexed.mapNotNull { record ->
            val lookup = lookups[record.revisionId] ?: return@mapNotNull null
            if (record.vector.dimensions != queryVector.dimensions) return@mapNotNull null
            val evidence = evidenceRows[record.revisionId]?.get(record.evidenceId)
                ?: return@mapNotNull null
            val excerpt = ResolveMeaningPdfOpenPage.truncateForEmbed(evidence.excerpt)
                .takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val rankedPage = PdfPageEvidenceLocator.parsePageNumber(evidence.locator)
            val cosine = EmbeddingSimilarity.cosine(queryVector, record.vector)
            val (score, tokenBoosted) = MeaningEvidenceTokenBoost.apply(
                cosine = cosine,
                query = query,
                evidenceText = excerpt,
            )
            if (score < MIN_CANDIDATE_SCORE) return@mapNotNull null
            MeaningSearchHit(
                revisionId = record.revisionId,
                memoryId = record.memoryId,
                sourceId = lookup.sourceId,
                sourceAssetKey = lookup.sourceAssetKey,
                assetType = lookup.assetType,
                label = lookup.displayLabel,
                summaryText = excerpt,
                citedPdfPageNumber = lookup.citedPdfPageNumber,
                rankedPdfPageNumber = rankedPage,
                score = score,
                model = model,
                evidenceTokenBoosted = tokenBoosted,
            )
        }

        val deduped = (summaryHits + evidenceHits)
            .groupBy { "${it.sourceId.value}|${it.sourceAssetKey.value}" }
            .values
            .map { group ->
                group.maxWithOrNull(
                    compareBy<MeaningSearchHit> { it.score }
                        .thenBy { if (it.rankedPdfPageNumber != null) 1 else 0 },
                )!!
            }
            .sortedByDescending { it.score }

        val limited = deduped.take(limit)
        MeaningSearchOutcome.Matches(
            query = query,
            hits = limited,
            limitReached = deduped.size > limit,
            model = model,
        )
    }

    companion object {
        const val DEFAULT_LIMIT = 10

        /** Soft floor so near-zero noise is not listed as a candidate. */
        const val MIN_CANDIDATE_SCORE = 0.05f
    }
}

data class MeaningSearchHit(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
    val assetType: AssetType,
    val label: String,
    val summaryText: String,
    val score: Float,
    val model: ModelVersionIdentity,
    /** 1-based PDF page cited by the Memory summary, when known. */
    val citedPdfPageNumber: Int? = null,
    /**
     * 1-based PDF page that won meaning ranking via an indexed evidence vector
     * whose locator is `pdf:page:N`. Null for summary-only / non-page hits.
     */
    val rankedPdfPageNumber: Int? = null,
    /** True when a significant cue token was found in evidence and boosted score. */
    val evidenceTokenBoosted: Boolean = false,
) {
    init {
        require(label.isNotBlank())
        require(summaryText.isNotBlank())
        require(score.isFinite())
        require(citedPdfPageNumber == null || citedPdfPageNumber > 0)
        require(rankedPdfPageNumber == null || rankedPdfPageNumber > 0)
    }
}

sealed interface MeaningSearchOutcome {
    data object BlankQuery : MeaningSearchOutcome

    data class EngineUnavailable(val reason: String) : MeaningSearchOutcome {
        init {
            require(reason.isNotBlank())
        }
    }

    data class NothingIndexed(val query: String) : MeaningSearchOutcome {
        init {
            require(query.isNotBlank())
        }
    }

    data class Failed(val reason: String) : MeaningSearchOutcome {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Matches(
        val query: String,
        val hits: List<MeaningSearchHit>,
        val limitReached: Boolean,
        val model: ModelVersionIdentity,
    ) : MeaningSearchOutcome {
        init {
            require(query.isNotBlank())
        }
    }
}
