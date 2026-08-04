package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.PdfPageEmbeddingStore
import com.memora.app.domain.memory.MemoryRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Honest inventory for the Find-by-meaning screen (on-device embedder path). */
class LoadMeaningSearchReadiness @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val embeddingStore: MemoryEmbeddingStore,
    private val pdfPageEmbeddingStore: PdfPageEmbeddingStore,
    private val memoryRepository: MemoryRepository,
) {
    suspend operator fun invoke(): MeaningSearchReadiness = withContext(Dispatchers.IO) {
        when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                MeaningSearchReadiness.EngineUnavailable(availability.reason)
            is CapabilityAvailability.Available -> {
                val indexed = embeddingStore.countForModel(availability.model) +
                    pdfPageEmbeddingStore.countForModel(availability.model)
                val memoriesReady = memoryRepository.countCurrentReady()
                MeaningSearchReadiness.Ready(
                    model = availability.model,
                    indexedCount = indexed,
                    memoriesReadyCount = memoriesReady,
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
    ) : MeaningSearchReadiness {
        init {
            require(indexedCount >= 0)
            require(memoriesReadyCount >= 0)
        }
    }
}
