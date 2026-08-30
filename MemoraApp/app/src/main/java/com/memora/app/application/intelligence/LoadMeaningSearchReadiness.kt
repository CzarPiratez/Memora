package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import com.memora.app.domain.memory.MemoryRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Honest inventory for the Find-by-meaning screen (on-device embedder path). */
class LoadMeaningSearchReadiness @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val loadCorpusCompleteness: LoadCorpusCompleteness,
) {
    suspend operator fun invoke(): MeaningSearchReadiness = withContext(Dispatchers.IO) {
        when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                MeaningSearchReadiness.EngineUnavailable(availability.reason)
            is CapabilityAvailability.Available -> {
                val snapshot = loadCorpusCompleteness()
                val counts = snapshot.counts
                MeaningSearchReadiness.Ready(
                    model = availability.model,
                    indexedCount = counts.meaningVectorsIndexed,
                    memoriesReadyCount = counts.memoriesReady,
                    corpusCompleteness = snapshot,
                )
            }
        }
    }
}

sealed interface MeaningSearchReadiness {
    data class EngineUnavailable(val reason: String) : MeaningSearchReadiness {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Ready(
        val model: ModelVersionIdentity,
        val indexedCount: Int,
        val memoriesReadyCount: Int,
        val corpusCompleteness: CorpusCompletenessSnapshot,
    ) : MeaningSearchReadiness {
        init {
            require(indexedCount >= 0)
            require(memoriesReadyCount >= 0)
        }
    }
}
