package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.MediaPipeAverageWordEmbedderSpec
import com.memora.app.domain.intelligence.MediaPipeUniversalSentenceEncoderSpec
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import javax.inject.Inject

/**
 * Removes meaning vectors that must not mix with the live product pack
 * (ADR-055 slice 4). USE / average-word rows are incompatible with BGE.
 */
class PurgeRetiredMeaningEmbeddings @Inject constructor(
    private val embeddingStore: MemoryEmbeddingStore,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
) {
    operator fun invoke(): PurgeRetiredMeaningEmbeddingsResult {
        var summaryDeleted = 0
        var evidenceDeleted = 0
        RETIRED_MODELS.forEach { model ->
            summaryDeleted += embeddingStore.deleteForModel(model)
            evidenceDeleted += evidenceEmbeddingStore.deleteForModel(model)
        }
        return PurgeRetiredMeaningEmbeddingsResult(
            summaryRowsDeleted = summaryDeleted,
            evidenceRowsDeleted = evidenceDeleted,
        )
    }

    companion object {
        val RETIRED_MODELS: List<ModelVersionIdentity> = listOf(
            MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY,
            MediaPipeAverageWordEmbedderSpec.MODEL_IDENTITY,
        )
    }
}

data class PurgeRetiredMeaningEmbeddingsResult(
    val summaryRowsDeleted: Int,
    val evidenceRowsDeleted: Int,
) {
    init {
        require(summaryRowsDeleted >= 0)
        require(evidenceRowsDeleted >= 0)
    }
}
