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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexPdfPageEmbeddingsTest {
    private val revisionId = MemoryRevisionId("rev-1")
    private val memoryId = MemoryId("mem-1")
    private val evidenceId = MemoryEvidenceId("e3")

    @Test
    fun unavailable_engine_writes_nothing_to_evidence_store() {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val result = IndexPdfPageEmbeddings(
            embeddingEngine = UnavailableEmbeddingEngine(),
            evidenceEmbeddingStore = evidenceStore,
        )(
            candidates = listOf(candidate(evidenceId = evidenceId)),
            nowEpochMs = 1L,
        )

        assertTrue(result is IndexPdfPageEmbeddingsResult.EngineUnavailable)
        assertEquals(0, evidenceStore.countForModel(ModelVersionIdentity("x", "1")))
    }

    @Test
    fun success_writes_evidence_store_with_real_evidence_id() {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val engine = FixedDimensionEmbeddingEngine()
        val useCase = IndexPdfPageEmbeddings(engine, evidenceStore)

        val result = useCase(
            candidates = listOf(candidate(evidenceId = evidenceId, pageText = "invoice total")),
            nowEpochMs = 10L,
        )

        assertTrue(result is IndexPdfPageEmbeddingsResult.Completed)
        val completed = result as IndexPdfPageEmbeddingsResult.Completed
        assertEquals(1, completed.indexed)
        assertEquals(0, completed.skippedUnchanged)
        assertEquals(0, completed.unresolvedEvidence)
        assertEquals(0, completed.failed)

        val evidence = evidenceStore.find(revisionId, evidenceId, engine.modelIdentity)
        assertNotNull(evidence)
        assertEquals("e3", evidence!!.evidenceId.value)
        assertTrue(PdfPageEvidenceLocatorGuard.isNotLocator(evidence.evidenceId))
    }

    @Test
    fun fingerprint_skip_when_evidence_already_current() {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val engine = FixedDimensionEmbeddingEngine()
        val useCase = IndexPdfPageEmbeddings(engine, evidenceStore)
        val pageText = "stable text"

        useCase(listOf(candidate(evidenceId = evidenceId, pageText = pageText)), nowEpochMs = 1L)
        val second = useCase(
            listOf(candidate(evidenceId = evidenceId, pageText = pageText)),
            nowEpochMs = 2L,
        )

        assertTrue(second is IndexPdfPageEmbeddingsResult.Completed)
        val completed = second as IndexPdfPageEmbeddingsResult.Completed
        assertEquals(0, completed.indexed)
        assertEquals(1, completed.skippedUnchanged)
        assertEquals(0, completed.unresolvedEvidence)
        assertEquals(1, evidenceStore.countForModel(engine.modelIdentity))
        assertEquals(1, engine.embedCalls)
    }

    @Test
    fun unresolved_evidence_id_writes_nothing_and_counts_fail() {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val engine = FixedDimensionEmbeddingEngine()
        val useCase = IndexPdfPageEmbeddings(engine, evidenceStore)

        val result = useCase(
            candidates = listOf(candidate(evidenceId = null, pageText = "orphan page")),
            nowEpochMs = 1L,
        )

        assertTrue(result is IndexPdfPageEmbeddingsResult.Completed)
        val completed = result as IndexPdfPageEmbeddingsResult.Completed
        assertEquals(0, completed.indexed)
        assertEquals(1, completed.unresolvedEvidence)
        assertEquals(1, completed.failed)
        assertEquals(0, evidenceStore.countForModel(engine.modelIdentity))
        assertEquals(0, engine.embedCalls)
    }

    @Test
    fun locator_shaped_evidence_id_is_rejected_and_does_not_corrupt_store() {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val engine = FixedDimensionEmbeddingEngine()
        val useCase = IndexPdfPageEmbeddings(engine, evidenceStore)

        val result = useCase(
            candidates = listOf(
                candidate(
                    evidenceId = MemoryEvidenceId("pdf:page:2"),
                    pageText = "must not key by locator",
                ),
            ),
            nowEpochMs = 1L,
        )

        assertTrue(result is IndexPdfPageEmbeddingsResult.Completed)
        val completed = result as IndexPdfPageEmbeddingsResult.Completed
        assertEquals(0, completed.indexed)
        assertEquals(1, completed.unresolvedEvidence)
        assertEquals(1, completed.failed)
        assertEquals(0, evidenceStore.countForModel(engine.modelIdentity))
        assertNull(evidenceStore.find(revisionId, MemoryEvidenceId("pdf:page:2"), engine.modelIdentity))
        assertEquals(0, engine.embedCalls)
    }

    private fun candidate(
        evidenceId: MemoryEvidenceId?,
        pageText: String = "page text",
        pageNumber: Int = 2,
    ) = PdfPageEmbeddingCandidate(
        revisionId = revisionId,
        memoryId = memoryId,
        pageNumber = pageNumber,
        pageText = pageText,
        evidenceId = evidenceId,
    )

    /** Local assert helper so tests do not depend on production parse import cycles. */
    private object PdfPageEvidenceLocatorGuard {
        fun isNotLocator(id: MemoryEvidenceId): Boolean =
            !Regex("""^pdf:page:\d+$""").matches(id.value.trim())
    }

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
    }
}
