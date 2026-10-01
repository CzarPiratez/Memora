package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.UnavailableEmbeddingEngine
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexOcrEvidenceEmbeddingsTest {
    private val revisionId = MemoryRevisionId("rev-ocr-1")
    private val memoryId = MemoryId("mem-ocr-1")
    private val evidenceId = MemoryEvidenceId("e-ocr-1")

    @Test
    fun unavailable_engine_writes_nothing_to_evidence_store() {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val result = IndexOcrEvidenceEmbeddings(
            embeddingEngine = UnavailableEmbeddingEngine(),
            evidenceEmbeddingStore = evidenceStore,
        )(
            candidates = listOf(candidate()),
            nowEpochMs = 1L,
        )

        assertTrue(result is IndexOcrEvidenceEmbeddingsResult.EngineUnavailable)
        assertEquals(0, evidenceStore.countForModel(ModelVersionIdentity("x", "1")))
    }

    @Test
    fun success_writes_evidence_store_with_real_evidence_id() {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val engine = FixedDimensionEmbeddingEngine()
        val useCase = IndexOcrEvidenceEmbeddings(engine, evidenceStore)

        val result = useCase(
            candidates = listOf(candidate(excerpt = "wifi password memo")),
            nowEpochMs = 10L,
        )

        assertTrue(result is IndexOcrEvidenceEmbeddingsResult.Completed)
        val completed = result as IndexOcrEvidenceEmbeddingsResult.Completed
        assertEquals(1, completed.indexed)
        assertEquals(0, completed.skippedUnchanged)
        assertEquals(0, completed.unresolvedEvidence)
        assertEquals(0, completed.failed)

        val evidence = evidenceStore.find(revisionId, evidenceId, engine.modelIdentity)
        assertNotNull(evidence)
        assertEquals("e-ocr-1", evidence!!.evidenceId.value)
    }

    @Test
    fun fingerprint_skip_when_ocr_text_already_current() {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val engine = FixedDimensionEmbeddingEngine()
        val useCase = IndexOcrEvidenceEmbeddings(engine, evidenceStore)
        val excerpt = "stable ocr text"

        useCase(listOf(candidate(excerpt = excerpt)), nowEpochMs = 1L)
        val second = useCase(
            listOf(candidate(excerpt = excerpt)),
            nowEpochMs = 2L,
        )

        assertTrue(second is IndexOcrEvidenceEmbeddingsResult.Completed)
        val completed = second as IndexOcrEvidenceEmbeddingsResult.Completed
        assertEquals(0, completed.indexed)
        assertEquals(1, completed.skippedUnchanged)
        assertEquals(1, evidenceStore.countForModel(engine.modelIdentity))
        assertEquals(1, engine.embedCalls)
    }

    @Test
    fun unresolved_evidence_id_counts_fail() {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val engine = FixedDimensionEmbeddingEngine()
        val useCase = IndexOcrEvidenceEmbeddings(engine, evidenceStore)

        val result = useCase(
            candidates = listOf(candidate(evidenceId = null)),
            nowEpochMs = 1L,
        )

        assertTrue(result is IndexOcrEvidenceEmbeddingsResult.Completed)
        val completed = result as IndexOcrEvidenceEmbeddingsResult.Completed
        assertEquals(0, completed.indexed)
        assertEquals(1, completed.unresolvedEvidence)
        assertEquals(1, completed.failed)
        assertEquals(0, engine.embedCalls)
    }

    private fun candidate(
        evidenceId: MemoryEvidenceId? = this.evidenceId,
        excerpt: String = "ocr excerpt",
    ) = OcrEvidenceEmbeddingCandidate(
        revisionId = revisionId,
        memoryId = memoryId,
        excerpt = excerpt,
        evidenceId = evidenceId,
    )

    private class FixedDimensionEmbeddingEngine(
        private val model: ModelVersionIdentity =
            ModelVersionIdentity(modelId = "fixed-embed-test", version = "0.0.1"),
    ) : EmbeddingEngine {
        var embedCalls = 0
            private set
        val modelIdentity: ModelVersionIdentity get() = model

        override fun availability(): CapabilityAvailability =
            CapabilityAvailability.Available(model)

        override fun limits(): CapabilityLimits? = null

        override fun embedText(text: String): EmbeddingEncodeResult {
            embedCalls += 1
            val values = FloatArray(4) { index ->
                ((text.hashCode() + index * 31) % 1000) / 1000f
            }
            return EmbeddingEncodeResult.Success(EmbeddingVector(values), model)
        }
    }

    private class InMemoryMemoryEvidenceEmbeddingStore : MemoryEvidenceEmbeddingStore {
        private val records = linkedMapOf<String, MemoryEvidenceEmbeddingRecord>()

        private fun key(
            revisionId: MemoryRevisionId,
            evidenceId: MemoryEvidenceId,
            model: ModelVersionIdentity,
        ) = "${revisionId.value}|${evidenceId.value}|${model.modelId}|${model.version}"

        override fun find(
            revisionId: MemoryRevisionId,
            evidenceId: MemoryEvidenceId,
            model: ModelVersionIdentity,
        ): MemoryEvidenceEmbeddingRecord? = records[key(revisionId, evidenceId, model)]

        override fun upsert(record: MemoryEvidenceEmbeddingRecord) {
            records[key(record.revisionId, record.evidenceId, record.model)] = record
        }

        override fun countForModel(model: ModelVersionIdentity): Int =
            records.values.count {
                it.model.modelId == model.modelId && it.model.version == model.version
            }

        override fun listForModel(model: ModelVersionIdentity): List<MemoryEvidenceEmbeddingRecord> =
            records.values.filter {
                it.model.modelId == model.modelId && it.model.version == model.version
            }

        override fun deleteForModel(model: ModelVersionIdentity): Int {
            val keysToRemove = records.filterValues {
                it.model.modelId == model.modelId && it.model.version == model.version
            }.keys.toList()
            keysToRemove.forEach { records.remove(it) }
            return keysToRemove.size
        }
    }
}
