package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingSimilarity
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Candidate Find-by-meaning over indexed Asset Memory summaries (E5b2).
 *
 * Compact-model path: ranks by cosine similarity only. Does not claim Local
 * Intelligence marketing AVAILABLE / SLA (ADR-024/025).
 */
class SearchAssetMemoriesByMeaning @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val embeddingStore: MemoryEmbeddingStore,
    private val memoryRepository: MemoryRepository,
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

        val indexed = embeddingStore.listForModel(model)
        if (indexed.isEmpty()) {
            return@withContext MeaningSearchOutcome.NothingIndexed(query = query)
        }

        val queryVector = when (val encoded = embeddingEngine.embedText(query)) {
            is EmbeddingEncodeResult.Unavailable ->
                return@withContext MeaningSearchOutcome.EngineUnavailable(encoded.reason)
            is EmbeddingEncodeResult.Failed ->
                return@withContext MeaningSearchOutcome.Failed(encoded.reason)
            is EmbeddingEncodeResult.Success -> encoded.vector
        }

        val lookups = memoryRepository.findCurrentReadyMeaningLookups(
            indexed.map { it.revisionId },
        )
        val scored = indexed.mapNotNull { record ->
            val lookup = lookups[record.revisionId] ?: return@mapNotNull null
            if (record.vector.dimensions != queryVector.dimensions) return@mapNotNull null
            val score = EmbeddingSimilarity.cosine(queryVector, record.vector)
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
                score = score,
                model = model,
            )
        }.sortedByDescending { it.score }

        val limited = scored.take(limit)
        MeaningSearchOutcome.Matches(
            query = query,
            hits = limited,
            limitReached = scored.size > limit,
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
) {
    init {
        require(label.isNotBlank())
        require(summaryText.isNotBlank())
        require(score.isFinite())
        require(citedPdfPageNumber == null || citedPdfPageNumber > 0)
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
