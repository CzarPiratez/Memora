package com.memora.app.application.intelligence

import com.memora.app.data.intelligence.NoBackupMeaningEncoderChallengerStore
import com.memora.app.data.intelligence.OnnxBiEncoderRuntime
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.BertWordPieceTokenizer
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingSimilarity
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.OnnxBgeSmallEnV15Spec
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceSearchRow
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ADR-055 slice 3 challenger probe. Re-embeds the same USE-indexed Memories
 * with BGE-small (incompatible vector space). Does **not** write Room, does
 * **not** swap the product [EmbeddingEngine], does **not** change Find.
 *
 * Corpus membership follows the product USE index so the cue set matches the
 * frozen USE card. Inference opens one ONNX session for the whole run.
 */
class ProbeMeaningEncoderChallenger @Inject constructor(
    @ApplicationContext context: Context,
    private val productEmbeddingEngine: EmbeddingEngine,
    private val embeddingStore: MemoryEmbeddingStore,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
    private val memoryRepository: MemoryRepository,
    private val challengerStore: NoBackupMeaningEncoderChallengerStore,
) {
    private val appContext = context.applicationContext

    suspend operator fun invoke(
        cues: List<MeaningEncoderProbeCue> = MeaningEncoderProbeCues.DEFAULT,
        poolLimit: Int = ProbeMeaningEncoderRanks.DEFAULT_POOL_LIMIT,
        onProgress: ((done: Int, total: Int) -> Unit)? = null,
    ): MeaningEncoderChallengerProbeReport = withContext(Dispatchers.Default) {
        require(poolLimit > 0)
        if (!challengerStore.isInstalled()) {
            return@withContext MeaningEncoderChallengerProbeReport.PackMissing
        }
        when (val availability = productEmbeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                return@withContext MeaningEncoderChallengerProbeReport.ProductEngineUnavailable(
                    availability.reason,
                )
            is CapabilityAvailability.Available -> Unit
        }
        if (cues.isEmpty()) {
            return@withContext MeaningEncoderChallengerProbeReport.Completed(
                rows = emptyList(),
                scorecard = MeaningEncoderBakeOffScorecard.of(emptyList()),
                verdict = MeaningEncoderBakeOffVerdict.decide(
                    live = MeaningEncoderUseBaseline.card,
                    challenger = MeaningEncoderBakeOffScorecard.ofRanks(
                        packId = OnnxBgeSmallEnV15Spec.MODEL_IDENTITY.modelId,
                        matchedAssetRanks = emptyList(),
                        unmatched = 0,
                    ),
                ),
            )
        }

        val productModel =
            (productEmbeddingEngine.availability() as CapabilityAvailability.Available).model
        val summaryIndexed = embeddingStore.listForModel(productModel)
        val evidenceIndexed = evidenceEmbeddingStore.listForModel(productModel)
        if (summaryIndexed.isEmpty() && evidenceIndexed.isEmpty()) {
            return@withContext MeaningEncoderChallengerProbeReport.NothingIndexed
        }

        val tokenizer = loadTokenizerOrNull()
            ?: return@withContext MeaningEncoderChallengerProbeReport.Failed(
                "BGE challenger tokenizer vocab is not available.",
            )
        val modelPath = challengerStore.absoluteModelPath()
            ?: return@withContext MeaningEncoderChallengerProbeReport.PackMissing

        val revisionIds = (summaryIndexed.map { it.revisionId } + evidenceIndexed.map { it.revisionId })
            .distinct()
        val lookups = memoryRepository.findMeaningIndexLookups(revisionIds)
        val evidenceRows = memoryRepository.findEvidenceSearchRows(revisionIds)

        val corpusTexts = linkedMapOf<CorpusKey, String>()
        for (record in summaryIndexed) {
            val lookup = lookups[record.revisionId] ?: continue
            val text = lookup.summaryText.trim()
            if (text.isBlank()) continue
            corpusTexts[
                CorpusKey(
                    revisionId = record.revisionId,
                    evidenceId = null,
                    sourceId = lookup.sourceId,
                    sourceAssetKey = lookup.sourceAssetKey,
                    label = lookup.displayLabel,
                ),
            ] = text
        }
        for (record in evidenceIndexed) {
            val lookup = lookups[record.revisionId] ?: continue
            val evidence = evidenceRows[record.revisionId]?.get(record.evidenceId) ?: continue
            val text = evidence.excerpt.trim()
            if (text.isBlank()) continue
            corpusTexts[
                CorpusKey(
                    revisionId = record.revisionId,
                    evidenceId = record.evidenceId,
                    sourceId = lookup.sourceId,
                    sourceAssetKey = lookup.sourceAssetKey,
                    label = lookup.displayLabel,
                ),
            ] = text
        }
        if (corpusTexts.isEmpty()) {
            return@withContext MeaningEncoderChallengerProbeReport.NothingIndexed
        }

        val embedTotal = corpusTexts.size + cues.size
        var embedDone = 0
        fun tick() {
            embedDone += 1
            onProgress?.invoke(embedDone, embedTotal)
        }

        try {
            val (env, session) = OnnxBiEncoderRuntime.openSession(File(modelPath))
            try {
                val vectors = LinkedHashMap<CorpusKey, EmbeddingVector>(corpusTexts.size)
                for ((key, text) in corpusTexts) {
                    val encoded = tokenizer.encodeSingle(
                        text = text,
                        maxLength = OnnxBgeSmallEnV15Spec.MAX_SEQUENCE_LENGTH,
                    )
                    vectors[key] = OnnxBiEncoderRuntime.embedEncoded(
                        env = env,
                        session = session,
                        encoded = encoded,
                        expectedDimensions = OnnxBgeSmallEnV15Spec.EMBEDDING_DIMENSIONS,
                    )
                    tick()
                }

                val rows = mutableListOf<MeaningEncoderProbeRow>()
                for (cue in cues) {
                    rows += probeOne(
                        cue = cue,
                        queryVectorProvider = {
                            val embedBody = MeaningRecallCue.embedText(cue.query)
                            if (embedBody.isBlank() ||
                                MeaningRecallCue.contentTokens(cue.query).isEmpty()
                            ) {
                                null
                            } else {
                                val encoded = tokenizer.encodeSingle(
                                    text = OnnxBgeSmallEnV15Spec.QUERY_PREFIX + embedBody,
                                    maxLength = OnnxBgeSmallEnV15Spec.MAX_SEQUENCE_LENGTH,
                                )
                                OnnxBiEncoderRuntime.embedEncoded(
                                    env = env,
                                    session = session,
                                    encoded = encoded,
                                    expectedDimensions = OnnxBgeSmallEnV15Spec.EMBEDDING_DIMENSIONS,
                                )
                            }
                        },
                        vectors = vectors,
                        lookups = lookups,
                        evidenceRows = evidenceRows,
                        poolLimit = poolLimit,
                    )
                    tick()
                }

                val scorecard = MeaningEncoderBakeOffScorecard.of(
                    rows = rows,
                    packId = OnnxBgeSmallEnV15Spec.MODEL_IDENTITY.modelId,
                    challenger = OnnxBgeSmallEnV15Spec.MODEL_IDENTITY.modelId,
                )
                val verdict = MeaningEncoderBakeOffVerdict.decide(
                    live = MeaningEncoderUseBaseline.card,
                    challenger = scorecard,
                )
                MeaningEncoderChallengerProbeReport.Completed(
                    rows = rows,
                    scorecard = scorecard,
                    verdict = verdict,
                )
            } finally {
                session.close()
            }
        } catch (error: Exception) {
            Log.w(LOG_TAG, "challenger probe failed: ${error.message}")
            MeaningEncoderChallengerProbeReport.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "BGE challenger probe failed on this phone.",
            )
        }
    }

    private fun probeOne(
        cue: MeaningEncoderProbeCue,
        queryVectorProvider: () -> EmbeddingVector?,
        vectors: Map<CorpusKey, EmbeddingVector>,
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
                vectorsScanned = vectors.size,
                skipped = "no content tokens",
            )
        }
        val queryVector = queryVectorProvider()
            ?: return MeaningEncoderProbeRow(
                query = display,
                embedText = embedText,
                contentTokens = contentTokens,
                vectorsScanned = vectors.size,
                skipped = "embed failed",
            )

        val chunks = mutableListOf<ScoredChunk>()
        for ((key, vector) in vectors) {
            if (vector.dimensions != queryVector.dimensions) continue
            val cosine = EmbeddingSimilarity.cosine(queryVector, vector)
            chunks += ScoredChunk(
                revisionId = key.revisionId,
                sourceId = key.sourceId,
                sourceAssetKey = key.sourceAssetKey,
                label = key.label,
                cosine = cosine,
                isEvidence = key.evidenceId != null,
                haystack = haystack(lookups[key.revisionId], evidenceRows[key.revisionId]),
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
            vectorsScanned = vectors.size,
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
        lookup: MemoryMeaningLookup?,
        rows: Map<MemoryEvidenceId, MemoryEvidenceSearchRow>?,
    ): String {
        if (lookup == null) return ""
        val excerpts = rows?.values.orEmpty().map { it.excerpt }
        return (listOf(lookup.summaryText, lookup.displayLabel, lookup.sourceAssetKey.value) + excerpts)
            .joinToString(" ")
    }

    private fun loadTokenizerOrNull(): BertWordPieceTokenizer? =
        try {
            appContext.assets.open(NoBackupMeaningEncoderChallengerStore.VOCAB_ASSET)
                .bufferedReader()
                .use { BertWordPieceTokenizer.loadFromReader(it) }
        } catch (error: Exception) {
            Log.w(LOG_TAG, "vocab load failed: ${error.message}")
            null
        }

    private data class CorpusKey(
        val revisionId: MemoryRevisionId,
        val evidenceId: MemoryEvidenceId?,
        val sourceId: SourceId,
        val sourceAssetKey: SourceAssetKey,
        val label: String,
    )

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
        const val LOG_TAG = "MeaningEncoderChallenger"
    }
}

sealed interface MeaningEncoderChallengerProbeReport {
    data object PackMissing : MeaningEncoderChallengerProbeReport

    data object NothingIndexed : MeaningEncoderChallengerProbeReport

    data class ProductEngineUnavailable(val reason: String) : MeaningEncoderChallengerProbeReport {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Failed(val reason: String) : MeaningEncoderChallengerProbeReport {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Completed(
        val rows: List<MeaningEncoderProbeRow>,
        val scorecard: MeaningEncoderBakeOffScorecard,
        val verdict: MeaningEncoderBakeOffVerdict,
    ) : MeaningEncoderChallengerProbeReport
}
