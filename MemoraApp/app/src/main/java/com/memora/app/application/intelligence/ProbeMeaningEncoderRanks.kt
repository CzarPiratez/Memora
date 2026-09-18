package com.memora.app.application.intelligence

import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingSimilarity
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceSearchRow
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read-only D-20 encoder probe. Cosine over the full USE index with **no**
 * token boost, precision tier, collapse-before-rank for the chunk number,
 * or live Find trim.
 *
 * Does not call MIG-05 cutover (that writes integrity). Does not change
 * [SearchAssetMemoriesByMeaning] ranking. Does not touch keyword Find.
 */
class ProbeMeaningEncoderRanks @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val embeddingStore: MemoryEmbeddingStore,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
    private val memoryRepository: MemoryRepository,
) {
    suspend operator fun invoke(
        cues: List<MeaningEncoderProbeCue> = MeaningEncoderProbeCues.DEFAULT,
        poolLimit: Int = DEFAULT_POOL_LIMIT,
    ): MeaningEncoderProbeReport = withContext(Dispatchers.Default) {
        require(poolLimit > 0)
        when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                return@withContext MeaningEncoderProbeReport.EngineUnavailable(availability.reason)
            is CapabilityAvailability.Available -> Unit
        }
        if (cues.isEmpty()) {
            return@withContext MeaningEncoderProbeReport.Completed(emptyList())
        }
        val model = (embeddingEngine.availability() as CapabilityAvailability.Available).model
        val summaryIndexed = embeddingStore.listForModel(model)
        val evidenceIndexed = evidenceEmbeddingStore.listForModel(model)
        if (summaryIndexed.isEmpty() && evidenceIndexed.isEmpty()) {
            return@withContext MeaningEncoderProbeReport.NothingIndexed
        }
        val revisionIds = (summaryIndexed.map { it.revisionId } + evidenceIndexed.map { it.revisionId })
            .distinct()
        val lookups = memoryRepository.findMeaningIndexLookups(revisionIds)
        val evidenceRows = memoryRepository.findEvidenceSearchRows(revisionIds)

        val rows = mutableListOf<MeaningEncoderProbeRow>()
        for (cue in cues) {
            rows += probeOne(
                cue = cue,
                queryVectorProvider = {
                    val embedQuery = MeaningRecallCue.embedText(cue.query)
                    if (embedQuery.isBlank() || MeaningRecallCue.contentTokens(cue.query).isEmpty()) {
                        null
                    } else {
                        when (val encoded = embeddingEngine.embedText(embedQuery)) {
                            is EmbeddingEncodeResult.Success -> encoded.vector
                            else -> null
                        }
                    }
                },
                summaryIndexed = summaryIndexed,
                evidenceIndexed = evidenceIndexed,
                lookups = lookups,
                evidenceRows = evidenceRows,
                poolLimit = poolLimit,
            )
        }
        MeaningEncoderProbeReport.Completed(rows)
    }

    private fun probeOne(
        cue: MeaningEncoderProbeCue,
        queryVectorProvider: () -> EmbeddingVector?,
        summaryIndexed: List<com.memora.app.domain.intelligence.MemoryEmbeddingRecord>,
        evidenceIndexed: List<com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord>,
        lookups: Map<MemoryRevisionId, MemoryMeaningLookup>,
        evidenceRows: Map<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>>,
        poolLimit: Int,
    ): MeaningEncoderProbeRow {
        val display = MeaningRecallCue.displayQuery(cue.query)
        val embedText = MeaningRecallCue.embedText(cue.query)
        val contentTokens = MeaningRecallCue.contentTokens(cue.query)
        if (contentTokens.isEmpty()) {
            return MeaningEncoderProbeRow(
                query = display,
                embedText = embedText,
                contentTokens = contentTokens,
                vectorsScanned = summaryIndexed.size + evidenceIndexed.size,
                skipped = "no content tokens",
            )
        }
        val queryVector = queryVectorProvider()
            ?: return MeaningEncoderProbeRow(
                query = display,
                embedText = embedText,
                contentTokens = contentTokens,
                vectorsScanned = summaryIndexed.size + evidenceIndexed.size,
                skipped = "embed failed",
            )

        val chunks = mutableListOf<ScoredChunk>()
        for (record in summaryIndexed) {
            val lookup = lookups[record.revisionId] ?: continue
            if (record.vector.dimensions != queryVector.dimensions) continue
            val cosine = EmbeddingSimilarity.cosine(queryVector, record.vector)
            chunks += ScoredChunk(
                revisionId = record.revisionId,
                sourceId = lookup.sourceId,
                sourceAssetKey = lookup.sourceAssetKey,
                label = lookup.displayLabel,
                cosine = cosine,
                isEvidence = false,
                haystack = haystack(lookup, evidenceRows[record.revisionId]),
            )
        }
        for (record in evidenceIndexed) {
            val lookup = lookups[record.revisionId] ?: continue
            val evidence = evidenceRows[record.revisionId]?.get(record.evidenceId) ?: continue
            if (record.vector.dimensions != queryVector.dimensions) continue
            val cosine = EmbeddingSimilarity.cosine(queryVector, record.vector)
            chunks += ScoredChunk(
                revisionId = record.revisionId,
                sourceId = lookup.sourceId,
                sourceAssetKey = lookup.sourceAssetKey,
                label = lookup.displayLabel,
                cosine = cosine,
                isEvidence = true,
                haystack = haystack(lookup, evidenceRows[record.revisionId]),
            )
        }

        val byCosine = chunks.sortedWith(
            compareByDescending<ScoredChunk> { it.cosine }
                .thenByDescending { if (it.isEvidence) 1 else 0 },
        )
        val goldNeedles = cue.goldSubstrings.map { it.lowercase() }
        val goldChunks = if (goldNeedles.isEmpty()) {
            emptyList()
        } else {
            byCosine.filter { it.matchesGold(goldNeedles) }
        }
        val chunkRank = goldChunks.firstOrNull()?.let { best ->
            byCosine.indexOfFirst { it.sameVector(best) } + 1
        }

        val collapsed = byCosine
            .filter { it.cosine >= SearchAssetMemoriesByMeaning.MIN_CANDIDATE_SCORE }
            .groupBy { it.assetKey }
            .values
            .map { group ->
                group.maxWith(
                    compareBy<ScoredChunk> { it.cosine }
                        .thenBy { if (it.isEvidence) 1 else 0 },
                )
            }
            .sortedByDescending { it.cosine }

        val goldAssets = if (goldNeedles.isEmpty()) {
            emptyList()
        } else {
            collapsed.filter { it.matchesGold(goldNeedles) }
        }
        val assetRank = goldAssets.firstOrNull()?.let { best ->
            collapsed.indexOfFirst { it.assetKey == best.assetKey } + 1
        }

        val prepared = MeaningEvidenceLexicalFilter.prepare(contentTokens)
        val tokenSeated = if (collapsed.size <= poolLimit || prepared.isEmpty) {
            collapsed.take(poolLimit)
        } else {
            collapsed
                .sortedByDescending { prepared.matchingTokens(it.haystack).size }
                .take(poolLimit)
        }
        val cosineSeated = collapsed.take(poolLimit)
        val goldAssetKey = goldAssets.firstOrNull()?.assetKey
        val inTokenPool = goldAssetKey != null && tokenSeated.any { it.assetKey == goldAssetKey }
        val inCosinePool = goldAssetKey != null && cosineSeated.any { it.assetKey == goldAssetKey }

        return MeaningEncoderProbeRow(
            query = display,
            embedText = embedText,
            contentTokens = contentTokens,
            vectorsScanned = summaryIndexed.size + evidenceIndexed.size,
            chunksScored = chunks.size,
            survivedFloor = collapsed.size,
            goldLabel = cue.goldSubstrings.joinToString(",").ifBlank { null },
            goldMatched = goldChunks.isNotEmpty(),
            chunkRank = chunkRank,
            goldChunkCosine = goldChunks.firstOrNull()?.cosine,
            assetRankAfterCollapse = assetRank,
            goldAssetCosine = goldAssets.firstOrNull()?.cosine,
            goldAssetLabel = goldAssets.firstOrNull()?.label,
            inTokenSeatedPool = if (goldAssetKey == null) null else inTokenPool,
            inCosineSeatedPool = if (goldAssetKey == null) null else inCosinePool,
            topCollapsedLabels = collapsed.take(5).map { it.label },
        )
    }

    private fun haystack(
        lookup: MemoryMeaningLookup,
        rows: Map<MemoryEvidenceId, MemoryEvidenceSearchRow>?,
    ): String {
        val excerpts = rows?.values.orEmpty().map { it.excerpt }
        return (listOf(lookup.summaryText, lookup.displayLabel, lookup.sourceAssetKey.value) + excerpts)
            .joinToString(" ")
    }

    private data class ScoredChunk(
        val revisionId: MemoryRevisionId,
        val sourceId: SourceId,
        val sourceAssetKey: SourceAssetKey,
        val label: String,
        val cosine: Float,
        val isEvidence: Boolean,
        val haystack: String,
    ) {
        val assetKey: String get() = "${sourceId.value}|${sourceAssetKey.value}"

        fun matchesGold(needles: List<String>): Boolean {
            val labelLower = label.lowercase()
            val keyLower = sourceAssetKey.value.lowercase()
            return needles.any { needle ->
                labelLower.contains(needle) || keyLower.contains(needle)
            }
        }

        fun sameVector(other: ScoredChunk): Boolean =
            revisionId == other.revisionId &&
                isEvidence == other.isEvidence &&
                cosine == other.cosine &&
                assetKey == other.assetKey
    }

    companion object {
        /** Mirrors [CanonicalRecall] MAX_CANDIDATE_POOL; membership only. */
        const val DEFAULT_POOL_LIMIT = 30

        const val LOG_TAG = "MeaningEncoderProbe"
    }
}

sealed interface MeaningEncoderProbeReport {
    data class EngineUnavailable(val reason: String) : MeaningEncoderProbeReport {
        init {
            require(reason.isNotBlank())
        }
    }

    data object NothingIndexed : MeaningEncoderProbeReport

    data class Completed(val rows: List<MeaningEncoderProbeRow>) : MeaningEncoderProbeReport
}

data class MeaningEncoderProbeRow(
    val query: String,
    val embedText: String,
    val contentTokens: List<String>,
    val vectorsScanned: Int,
    val chunksScored: Int = 0,
    val survivedFloor: Int = 0,
    val goldLabel: String? = null,
    val goldMatched: Boolean = false,
    val chunkRank: Int? = null,
    val goldChunkCosine: Float? = null,
    val assetRankAfterCollapse: Int? = null,
    val goldAssetCosine: Float? = null,
    val goldAssetLabel: String? = null,
    val inTokenSeatedPool: Boolean? = null,
    val inCosineSeatedPool: Boolean? = null,
    val topCollapsedLabels: List<String> = emptyList(),
    val skipped: String? = null,
) {
    fun logLine(): String = buildString {
        append("query=\"").append(query).append('"')
        append(" embed=\"").append(embedText).append('"')
        append(" scanned=").append(vectorsScanned)
        append(" scored=").append(chunksScored)
        append(" floor=").append(survivedFloor)
        skipped?.let { append(" skipped=").append(it); return@buildString }
        append(" gold=").append(goldLabel ?: "-")
        append(" matched=").append(goldMatched)
        append(" chunkRank=").append(chunkRank ?: "-")
        append(" chunkCos=").append(goldChunkCosine ?: "-")
        append(" assetRank=").append(assetRankAfterCollapse ?: "-")
        append(" assetCos=").append(goldAssetCosine ?: "-")
        append(" token30=").append(inTokenSeatedPool?.toString() ?: "-")
        append(" cosine30=").append(inCosineSeatedPool?.toString() ?: "-")
        append(" top=").append(topCollapsedLabels.joinToString("|"))
    }
}
