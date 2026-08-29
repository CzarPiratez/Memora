package com.memora.app.domain.intelligence

import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MIG-05 step 1: evidence-level embedding store contract (put/get/list + empty honesty).
 * Does not exercise Room; production Room adapter is covered by migration instrumentation.
 */
class MemoryEvidenceEmbeddingStoreTest {
    private val model = ModelVersionIdentity(modelId = "test-embed", version = "0.0.1")
    private val revisionId = MemoryRevisionId("rev-1")
    private val memoryId = MemoryId("mem-1")

    @Test
    fun empty_store_is_honest() {
        val store = InMemoryMemoryEvidenceEmbeddingStore()

        assertNull(
            store.find(
                revisionId = revisionId,
                evidenceId = MemoryEvidenceId("e1"),
                model = model,
            ),
        )
        assertEquals(0, store.countForModel(model))
        assertTrue(store.listForModel(model).isEmpty())
    }

    @Test
    fun upsert_and_find_by_non_pdf_shaped_evidence_id() {
        val store = InMemoryMemoryEvidenceEmbeddingStore()
        val evidenceId = MemoryEvidenceId("e1")
        val record = sampleRecord(evidenceId = evidenceId, fingerprint = "fp-ocr")

        store.upsert(record)

        val found = store.find(revisionId, evidenceId, model)
        assertEquals(record, found)
        assertEquals(1, store.countForModel(model))
    }

    @Test
    fun upsert_and_find_by_pdf_shaped_evidence_id_string() {
        val store = InMemoryMemoryEvidenceEmbeddingStore()
        val evidenceId = MemoryEvidenceId("pdf:page:3")
        val record = sampleRecord(evidenceId = evidenceId, fingerprint = "fp-pdf-page-3")

        store.upsert(record)

        val found = store.find(revisionId, evidenceId, model)
        assertEquals(record, found)
        assertEquals("pdf:page:3", found?.evidenceId?.value)
    }

    @Test
    fun list_for_model_returns_both_evidence_shapes() {
        val store = InMemoryMemoryEvidenceEmbeddingStore()
        val nonPdf = sampleRecord(evidenceId = MemoryEvidenceId("e1"), fingerprint = "fp-a")
        val pdfShaped = sampleRecord(
            evidenceId = MemoryEvidenceId("pdf:page:2"),
            fingerprint = "fp-b",
            createdAtEpochMs = 2L,
        )

        store.upsert(nonPdf)
        store.upsert(pdfShaped)

        val listed = store.listForModel(model)
        assertEquals(2, listed.size)
        assertTrue(listed.any { it.evidenceId.value == "e1" })
        assertTrue(listed.any { it.evidenceId.value == "pdf:page:2" })
        assertEquals(2, store.countForModel(model))
    }

    @Test
    fun find_misses_when_evidence_id_differs() {
        val store = InMemoryMemoryEvidenceEmbeddingStore()
        store.upsert(sampleRecord(evidenceId = MemoryEvidenceId("e1"), fingerprint = "fp"))

        assertNull(
            store.find(
                revisionId = revisionId,
                evidenceId = MemoryEvidenceId("e2"),
                model = model,
            ),
        )
    }

    private fun sampleRecord(
        evidenceId: MemoryEvidenceId,
        fingerprint: String,
        createdAtEpochMs: Long = 1L,
    ): MemoryEvidenceEmbeddingRecord =
        MemoryEvidenceEmbeddingRecord(
            revisionId = revisionId,
            memoryId = memoryId,
            evidenceId = evidenceId,
            model = model,
            vector = EmbeddingVector(floatArrayOf(0.1f, 0.2f, 0.3f)),
            sourceTextFingerprint = fingerprint,
            createdAtEpochMs = createdAtEpochMs,
        )

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
