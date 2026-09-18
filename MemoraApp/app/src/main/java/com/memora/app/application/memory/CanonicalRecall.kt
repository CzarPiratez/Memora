package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.application.intelligence.MeaningSearchTrace
import com.memora.app.application.intelligence.SearchAssetMemoriesByMeaning
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.RecallPrecision
import com.memora.app.domain.intelligence.RecallRanker
import com.memora.app.domain.memory.MemoryRepository
import javax.inject.Inject
import java.util.concurrent.TimeUnit

/**
 * App product-facing retrieval boundary (ADR-049).
 *
 * KEYWORD path delegates to [SearchMemoryEvidence]. MEANING path (MIG-07B
 * Slice 2) delegates to [SearchAssetMemoriesByMeaning] with anchor-aware
 * structured filter and FC-02 Stage A [RecallRanker].
 *
 * Meaning Find ViewModels call [searchByMeaning] (MIG-07B Slice 3). Candidate
 * generation remains [SearchAssetMemoriesByMeaning]; shared ranking (token boost
 * + lexical + Stage A rerank + anchor filter) lives in [AnchorAwareMeaningRecallRanking].
 */
class CanonicalRecall @Inject constructor(
    private val searchMemoryEvidence: SearchMemoryEvidence,
    private val searchAssetMemoriesByMeaning: SearchAssetMemoriesByMeaning,
    private val memoryRepository: MemoryRepository,
    private val recallRanker: RecallRanker,
) {
    /**
     * Keyword / literal recall over stored Memory evidence.
     *
     * @param assetType optional scope for per-asset Find screens; null searches
     *   all asset types.
     */
    suspend operator fun invoke(
        rawQuery: String,
        limit: Int = MemoryEvidenceLiteralSearchSupport.MAX_RESULTS,
        assemblySchemaVersion: String = SearchMemoryEvidence.DEFAULT_ASSEMBLY_SCHEMA,
        assetType: AssetType? = null,
    ): MemoryEvidenceSearchOutcome =
        searchMemoryEvidence(
            rawQuery = rawQuery,
            limit = limit,
            assemblySchemaVersion = assemblySchemaVersion,
            assetType = assetType,
        )

    /**
     * Meaning recall with MIG-07B anchor structured-filter stage applied when
     * the query carries TIME/TOPIC cues.
     */
    suspend fun searchByMeaning(
        rawQuery: String,
        limit: Int = MeaningTrustedHitPolicy.MAX_TRUSTED_HITS,
    ): MeaningSearchOutcome {
        val startedNs = System.nanoTime()
        val poolLimit = (limit * CANDIDATE_POOL_MULTIPLIER).coerceAtMost(MAX_CANDIDATE_POOL)
        val outcome = searchAssetMemoriesByMeaning(rawQuery = rawQuery, limit = poolLimit)
        val ranked = AnchorAwareMeaningRecallRanking.apply(
            outcome = outcome,
            rawQuery = rawQuery,
            memoryRepository = memoryRepository,
            recallRanker = recallRanker,
        )
        val trimmed = trimMeaningMatches(ranked, limit)
        MeaningSearchTrace.emit(
            MeaningSearchTrace.fromLivePath(
                rawQuery = rawQuery,
                candidate = outcome,
                ranked = ranked,
                shown = trimmed,
                latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNs),
            ),
        )
        return trimmed
    }

    private fun trimMeaningMatches(
        outcome: MeaningSearchOutcome,
        limit: Int,
    ): MeaningSearchOutcome {
        if (outcome !is MeaningSearchOutcome.Matches) return outcome
        val page = MeaningTrustedHitPolicy.page(outcome.hits, limit)
        if (page.hits.isEmpty()) {
            return outcome.copy(
                hits = emptyList(),
                limitReached = false,
                precision = RecallPrecision.Exact,
            )
        }
        val (refined, precision) = AnchorAwareMeaningRecallRanking.refineAfterTrustedTrim(
            hits = page.hits,
            rawQuery = outcome.query,
        )
        if (refined.isEmpty()) {
            return outcome.copy(
                hits = emptyList(),
                limitReached = false,
                precision = RecallPrecision.Exact,
            )
        }
        // Cap only: far cosine neighbours and a full 60-pool must not look
        // like "we hid more equally close files."
        return outcome.copy(
            hits = refined,
            limitReached = page.truncatedByPageCap,
            precision = precision,
        )
    }

    companion object {
        /**
         * Over-fetch candidates so later ranking can promote lower-cosine hits
         * before the shown page (up to [MeaningTrustedHitPolicy.MAX_TRUSTED_HITS])
         * is applied. Must stay larger than that page. This is not a second
         * product list and must not grow with corpus size.
         */
        private const val CANDIDATE_POOL_MULTIPLIER = 3

        private const val MAX_CANDIDATE_POOL = 60
    }

    /**
     * Hybrid keyword + meaning recall with reciprocal rank fusion. Not yet
     * wired to product Find UI — infrastructure for converged recall.
     */
    suspend fun searchHybrid(
        rawQuery: String,
        limit: Int = MemoryEvidenceLiteralSearchSupport.MAX_RESULTS,
        assemblySchemaVersion: String = SearchMemoryEvidence.DEFAULT_ASSEMBLY_SCHEMA,
        assetType: AssetType? = null,
    ): CanonicalRecallHybridOutcome {
        val keywordOutcome = searchMemoryEvidence(
            rawQuery = rawQuery,
            limit = limit,
            assemblySchemaVersion = assemblySchemaVersion,
            assetType = assetType,
        )
        val meaningOutcome = searchByMeaning(rawQuery = rawQuery, limit = limit)

        val query = when {
            keywordOutcome is MemoryEvidenceSearchOutcome.BlankQuery -> return CanonicalRecallHybridOutcome.BlankQuery
            meaningOutcome is MeaningSearchOutcome.BlankQuery -> return CanonicalRecallHybridOutcome.BlankQuery
            keywordOutcome is MemoryEvidenceSearchOutcome.Matches -> keywordOutcome.query
            meaningOutcome is MeaningSearchOutcome.Matches -> meaningOutcome.query
            meaningOutcome is MeaningSearchOutcome.NothingIndexed -> meaningOutcome.query
            keywordOutcome is MemoryEvidenceSearchOutcome.NothingSavedToSearch -> keywordOutcome.query
            else -> rawQuery.trim().takeIf { it.isNotEmpty() }
                ?: return CanonicalRecallHybridOutcome.BlankQuery
        }

        val keywordKeys = when (keywordOutcome) {
            is MemoryEvidenceSearchOutcome.Matches ->
                keywordOutcome.hits.map {
                    ReciprocalRankFusion.fusionKey(it.sourceId, it.sourceAssetKey)
                }
            else -> emptyList()
        }
        val meaningKeys = when (meaningOutcome) {
            is MeaningSearchOutcome.Matches ->
                meaningOutcome.hits.map {
                    ReciprocalRankFusion.fusionKey(it.sourceId, it.sourceAssetKey)
                }
            else -> emptyList()
        }

        if (keywordKeys.isEmpty() && meaningKeys.isEmpty()) {
            return CanonicalRecallHybridOutcome.NoMatches(query = query)
        }

        val fused = ReciprocalRankFusion.fuse(
            keywordKeys = keywordKeys,
            meaningKeys = meaningKeys,
        ).take(limit)

        return CanonicalRecallHybridOutcome.Matches(
            query = query,
            entries = fused.map { entry ->
                val (sourceId, sourceAssetKey) = parseFusionKey(entry.fusionKey)
                CanonicalRecallFusionEntry(
                    sourceId = sourceId,
                    sourceAssetKey = sourceAssetKey,
                    fusionScore = entry.score,
                    keywordRank = entry.keywordRank,
                    meaningRank = entry.meaningRank,
                )
            },
            limitReached = fused.size >= limit,
        )
    }

    private fun parseFusionKey(key: String): Pair<SourceId, SourceAssetKey> {
        val parts = key.split('\u001f', limit = 2)
        require(parts.size == 2) { "Invalid fusion key: $key" }
        return SourceId(parts[0]) to SourceAssetKey(parts[1])
    }
}

sealed interface CanonicalRecallHybridOutcome {
    data object BlankQuery : CanonicalRecallHybridOutcome

    data class NoMatches(val query: String) : CanonicalRecallHybridOutcome {
        init {
            require(query.isNotBlank())
        }
    }

    data class Matches(
        val query: String,
        val entries: List<CanonicalRecallFusionEntry>,
        val limitReached: Boolean,
    ) : CanonicalRecallHybridOutcome {
        init {
            require(query.isNotBlank())
        }
    }
}

data class CanonicalRecallFusionEntry(
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
    val fusionScore: Float,
    val keywordRank: Int?,
    val meaningRank: Int?,
) {
    init {
        require(fusionScore.isFinite())
        require(keywordRank == null || keywordRank > 0)
        require(meaningRank == null || meaningRank > 0)
    }
}
