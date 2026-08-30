package com.memora.app.application.intelligence

import com.memora.app.application.memory.AssembleAssetMemoryFromExtractionFacts
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.CorpusCompletenessBlocked
import com.memora.app.domain.memory.CorpusCompletenessCounts
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import com.memora.app.domain.memory.MemoryRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads indexed / pending / blocked counts for meaning-search corpus honesty (FC-04).
 */
class LoadCorpusCompleteness @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val factSource: AssetMemoryFactSource,
    private val embeddingEngine: EmbeddingEngine,
    private val embeddingStore: MemoryEmbeddingStore,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
    private val applyMig05EvidenceSearchCutover: ApplyMig05EvidenceSearchCutover,
) {
    suspend operator fun invoke(): CorpusCompletenessSnapshot = withContext(Dispatchers.IO) {
        val memoriesReady = memoryRepository.countCurrentReady()
        val memoriesPendingAssembly = factSource.countPendingAssembly(
            AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA,
        )
        val meaningIndexCandidates = memoryRepository.countMeaningIndexCandidates()

        val model = when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Available -> {
                applyMig05EvidenceSearchCutover.ensureApplied(availability.model)
                availability.model
            }
            is CapabilityAvailability.Unavailable -> null
        }

        val (summaryIndexed, evidenceIndexed) = if (model != null) {
            embeddingStore.countForModel(model) to evidenceEmbeddingStore.countForModel(model)
        } else {
            0 to 0
        }

        val meaningIndexPending = if (model != null) {
            (meaningIndexCandidates - summaryIndexed).coerceAtLeast(0)
        } else {
            meaningIndexCandidates.coerceAtLeast(0)
        }

        val counts = CorpusCompletenessCounts(
            memoriesReady = memoriesReady,
            memoriesPendingAssembly = memoriesPendingAssembly,
            meaningSummaryIndexed = summaryIndexed,
            meaningEvidenceIndexed = evidenceIndexed,
            meaningIndexPending = meaningIndexPending,
        )
        CorpusCompletenessSnapshot(
            counts = counts,
            blocked = resolveBlocked(counts),
        )
    }

    private fun resolveBlocked(counts: CorpusCompletenessCounts): CorpusCompletenessBlocked? =
        when {
            counts.memoriesReady == 0 && counts.memoriesPendingAssembly > 0 ->
                CorpusCompletenessBlocked.BuildMemoriesFirst
            counts.memoriesReady > 0 && counts.meaningVectorsIndexed == 0 ->
                CorpusCompletenessBlocked.BuildMeaningIndex
            else -> null
        }
}
