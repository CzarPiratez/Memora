package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MediaPipeUniversalSentenceEncoderSpec
import com.memora.app.domain.intelligence.MemoryEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.OnnxBgeSmallEnV15Spec
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PurgeRetiredMeaningEmbeddingsTest {
    @Test
    fun removes_use_and_average_word_rows_but_keeps_bge() {
        val summaries = InMemoryMemoryEmbeddingStore()
        val evidence = InMemoryMemoryEvidenceEmbeddingStore()
        summaries.upsert(record(MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY, "use-sum"))
        summaries.upsert(record(OnnxBgeSmallEnV15Spec.MODEL_IDENTITY, "bge-sum"))
        evidence.upsert(
            evidenceRecord(MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY, "use-ev"),
        )
        evidence.upsert(
            evidenceRecord(OnnxBgeSmallEnV15Spec.MODEL_IDENTITY, "bge-ev"),
        )

        val result = PurgeRetiredMeaningEmbeddings(summaries, evidence)()

        assertEquals(1, result.summaryRowsDeleted)
        assertEquals(1, result.evidenceRowsDeleted)
        assertEquals(0, summaries.countForModel(MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY))
        assertEquals(1, summaries.countForModel(OnnxBgeSmallEnV15Spec.MODEL_IDENTITY))
        assertEquals(0, evidence.countForModel(MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY))
        assertEquals(1, evidence.countForModel(OnnxBgeSmallEnV15Spec.MODEL_IDENTITY))
        assertTrue(summaries.listForModel(OnnxBgeSmallEnV15Spec.MODEL_IDENTITY).isNotEmpty())
    }

    private fun record(model: ModelVersionIdentity, fingerprint: String) =
        MemoryEmbeddingRecord(
            revisionId = MemoryRevisionId("rev-$fingerprint"),
            memoryId = MemoryId("mem-$fingerprint"),
            model = model,
            vector = EmbeddingVector(floatArrayOf(1f, 0f)),
            sourceTextFingerprint = fingerprint,
            createdAtEpochMs = 1L,
        )

    private fun evidenceRecord(model: ModelVersionIdentity, fingerprint: String) =
        MemoryEvidenceEmbeddingRecord(
            revisionId = MemoryRevisionId("rev-$fingerprint"),
            memoryId = MemoryId("mem-$fingerprint"),
            evidenceId = MemoryEvidenceId("ev-$fingerprint"),
            model = model,
            vector = EmbeddingVector(floatArrayOf(0f, 1f)),
            sourceTextFingerprint = fingerprint,
            createdAtEpochMs = 1L,
        )

    private class InMemoryMemoryEmbeddingStore : MemoryEmbeddingStore {
        private val rows = mutableListOf<MemoryEmbeddingRecord>()

        override fun find(revisionId: MemoryRevisionId, model: ModelVersionIdentity) =
            rows.firstOrNull { it.revisionId == revisionId && it.model == model }

        override fun upsert(record: MemoryEmbeddingRecord) {
            rows.removeAll { it.revisionId == record.revisionId && it.model == record.model }
            rows += record
        }

        override fun countForModel(model: ModelVersionIdentity) =
            rows.count { it.model == model }

        override fun listForModel(model: ModelVersionIdentity) =
            rows.filter { it.model == model }

        override fun deleteForModel(model: ModelVersionIdentity): Int {
            val before = rows.size
            rows.removeAll { it.model == model }
            return before - rows.size
        }
    }

    private class InMemoryMemoryEvidenceEmbeddingStore : MemoryEvidenceEmbeddingStore {
        private val rows = mutableListOf<MemoryEvidenceEmbeddingRecord>()

        override fun find(
            revisionId: MemoryRevisionId,
            evidenceId: MemoryEvidenceId,
            model: ModelVersionIdentity,
        ) = rows.firstOrNull {
            it.revisionId == revisionId && it.evidenceId == evidenceId && it.model == model
        }

        override fun upsert(record: MemoryEvidenceEmbeddingRecord) {
            rows.removeAll {
                it.revisionId == record.revisionId &&
                    it.evidenceId == record.evidenceId &&
                    it.model == record.model
            }
            rows += record
        }

        override fun countForModel(model: ModelVersionIdentity) =
            rows.count { it.model == model }

        override fun listForModel(model: ModelVersionIdentity) =
            rows.filter { it.model == model }

        override fun deleteForModel(model: ModelVersionIdentity): Int {
            val before = rows.size
            rows.removeAll { it.model == model }
            return before - rows.size
        }
    }
}
