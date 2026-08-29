package com.memora.app.data.local

import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MIG-05 step 1: Room adapter put/get/list via an in-memory fake DAO. */
class RoomMemoryEvidenceEmbeddingStoreTest {
    private val model = ModelVersionIdentity(modelId = "test-embed", version = "0.0.1")
    private val revisionId = MemoryRevisionId("rev-room-1")
    private val memoryId = MemoryId("mem-room-1")

    @Test
    fun empty_room_adapter_is_honest() {
        val store = RoomMemoryEvidenceEmbeddingStore(dao = { FakeDao() })

        assertNull(store.find(revisionId, MemoryEvidenceId("e1"), model))
        assertEquals(0, store.countForModel(model))
        assertTrue(store.listForModel(model).isEmpty())
    }

    @Test
    fun room_adapter_round_trips_non_pdf_and_pdf_shaped_evidence_ids() {
        val dao = FakeDao()
        val store = RoomMemoryEvidenceEmbeddingStore(dao = { dao })
        val nonPdf = record(MemoryEvidenceId("e1"), "fp-ocr")
        val pdfShaped = record(MemoryEvidenceId("pdf:page:3"), "fp-page", createdAt = 2L)

        store.upsert(nonPdf)
        store.upsert(pdfShaped)

        assertEquals(nonPdf, store.find(revisionId, MemoryEvidenceId("e1"), model))
        assertEquals(pdfShaped, store.find(revisionId, MemoryEvidenceId("pdf:page:3"), model))
        assertEquals(2, store.countForModel(model))
        val listed = store.listForModel(model)
        assertEquals(2, listed.size)
        assertTrue(listed.any { it.evidenceId.value == "e1" })
        assertTrue(listed.any { it.evidenceId.value == "pdf:page:3" })
    }

    private fun record(
        evidenceId: MemoryEvidenceId,
        fingerprint: String,
        createdAt: Long = 1L,
    ): MemoryEvidenceEmbeddingRecord =
        MemoryEvidenceEmbeddingRecord(
            revisionId = revisionId,
            memoryId = memoryId,
            evidenceId = evidenceId,
            model = model,
            vector = EmbeddingVector(floatArrayOf(0.25f, 0.5f)),
            sourceTextFingerprint = fingerprint,
            createdAtEpochMs = createdAt,
        )

    private class FakeDao : MemoryEvidenceEmbeddingDao {
        private val rows = linkedMapOf<String, MemoryEvidenceEmbeddingEntity>()

        private fun key(
            revisionId: String,
            evidenceId: String,
            modelId: String,
            modelVersion: String,
        ) = "$revisionId|$evidenceId|$modelId|$modelVersion"

        override suspend fun find(
            revisionId: String,
            evidenceId: String,
            modelId: String,
            modelVersion: String,
        ): MemoryEvidenceEmbeddingEntity? =
            rows[key(revisionId, evidenceId, modelId, modelVersion)]

        override suspend fun upsert(entity: MemoryEvidenceEmbeddingEntity) {
            rows[
                key(
                    entity.revisionId,
                    entity.evidenceId,
                    entity.modelId,
                    entity.modelVersion,
                ),
            ] = entity
        }

        override suspend fun countForModel(modelId: String, modelVersion: String): Int =
            rows.values.count { it.modelId == modelId && it.modelVersion == modelVersion }

        override suspend fun listForModel(
            modelId: String,
            modelVersion: String,
        ): List<MemoryEvidenceEmbeddingEntity> =
            rows.values
                .filter { it.modelId == modelId && it.modelVersion == modelVersion }
                .sortedByDescending { it.createdAtEpochMs }
    }
}
