package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.UnavailableEmbeddingEngine
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexMemoryEmbeddingsTest {
    @Test
    fun product_unavailable_engine_writes_nothing() {
        val store = InMemoryMemoryEmbeddingStore()
        val result = IndexMemoryEmbeddings(
            embeddingEngine = UnavailableEmbeddingEngine(),
            embeddingStore = store,
        )(
            candidates = listOf(
                MemoryEmbeddingCandidate(
                    revisionId = MemoryRevisionId("rev-1"),
                    memoryId = MemoryId("mem-1"),
                    summaryText = "coffee shop receipt",
                ),
            ),
            nowEpochMs = 1L,
        )

        assertTrue(result is IndexMemoryEmbeddingsResult.EngineUnavailable)
        assertEquals(0, store.countForModel(ModelVersionIdentity("x", "1")))
    }

    @Test
    fun available_engine_indexes_and_skips_unchanged() {
        val store = InMemoryMemoryEmbeddingStore()
        val engine = FixedDimensionEmbeddingEngine()
        val useCase = IndexMemoryEmbeddings(engine, store)
        val candidate = MemoryEmbeddingCandidate(
            revisionId = MemoryRevisionId("rev-1"),
            memoryId = MemoryId("mem-1"),
            summaryText = "coffee shop receipt",
        )

        val first = useCase(listOf(candidate), nowEpochMs = 1L)
        assertTrue(first is IndexMemoryEmbeddingsResult.Completed)
        assertEquals(1, (first as IndexMemoryEmbeddingsResult.Completed).indexed)

        val second = useCase(listOf(candidate), nowEpochMs = 2L)
        assertTrue(second is IndexMemoryEmbeddingsResult.Completed)
        assertEquals(0, (second as IndexMemoryEmbeddingsResult.Completed).indexed)
        assertEquals(1, second.skippedUnchanged)
        assertEquals(1, store.countForModel(engine.modelIdentity))
    }

    private class FixedDimensionEmbeddingEngine(
        private val model: ModelVersionIdentity =
            ModelVersionIdentity(modelId = "fixed-embed-test", version = "0.0.1"),
    ) : EmbeddingEngine {
        val modelIdentity: ModelVersionIdentity get() = model

        override fun availability(): CapabilityAvailability =
            CapabilityAvailability.Available(model)

        override fun limits(): CapabilityLimits? = null

        override fun embedText(text: String): EmbeddingEncodeResult {
            val values = FloatArray(4) { index ->
                ((text.hashCode() + index * 31) % 1000) / 1000f
            }
            return EmbeddingEncodeResult.Success(EmbeddingVector(values), model)
        }
    }

    private class InMemoryMemoryEmbeddingStore : MemoryEmbeddingStore {
        private val records = linkedMapOf<String, MemoryEmbeddingRecord>()

        private fun key(revisionId: MemoryRevisionId, model: ModelVersionIdentity) =
            "${revisionId.value}|${model.modelId}|${model.version}"

        override fun find(
            revisionId: MemoryRevisionId,
            model: ModelVersionIdentity,
        ): MemoryEmbeddingRecord? = records[key(revisionId, model)]

        override fun upsert(record: MemoryEmbeddingRecord) {
            records[key(record.revisionId, record.model)] = record
        }

        override fun countForModel(model: ModelVersionIdentity): Int =
            records.values.count {
                it.model.modelId == model.modelId && it.model.version == model.version
            }

        override fun listForModel(model: ModelVersionIdentity): List<MemoryEmbeddingRecord> =
            records.values.filter {
                it.model.modelId == model.modelId && it.model.version == model.version
            }
    }
}
