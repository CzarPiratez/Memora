package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.UnavailableEmbeddingEngine
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryEmbeddingSummary
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceSearchRow
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryInsertResult
import com.memora.app.domain.memory.MemoryIntegrityState
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadMeaningSearchReadinessTest {
    private val model = ModelVersionIdentity("test-embedder", "1")

    @Test
    fun unavailable_engine_short_circuits() = runBlocking {
        val readiness = LoadMeaningSearchReadiness(
            embeddingEngine = UnavailableEmbeddingEngine("missing"),
            embeddingStore = InMemoryMemoryEmbeddingStore(),
            evidenceEmbeddingStore = InMemoryMemoryEvidenceEmbeddingStore(),
            memoryRepository = FakeMemoryRepository(),
            applyMig05EvidenceSearchCutover = ApplyMig05EvidenceSearchCutover(
                memoryRepository = FakeMemoryRepository(),
                evidenceEmbeddingStore = InMemoryMemoryEvidenceEmbeddingStore(),
            ),
        )()
        assertTrue(readiness is MeaningSearchReadiness.EngineUnavailable)
    }

    @Test
    fun indexed_count_is_summary_plus_evidence_store() = runBlocking {
        val summaryStore = InMemoryMemoryEmbeddingStore()
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val revision = MemoryRevisionId("rev-1")
        summaryStore.upsert(
            MemoryEmbeddingRecord(
                revisionId = revision,
                memoryId = MemoryId("mem-1"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f)),
                sourceTextFingerprint = "fp-s",
                createdAtEpochMs = 1L,
            ),
        )
        evidenceStore.upsert(
            MemoryEvidenceEmbeddingRecord(
                revisionId = revision,
                memoryId = MemoryId("mem-1"),
                evidenceId = MemoryEvidenceId("e1"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(0f, 1f)),
                sourceTextFingerprint = "fp-e",
                createdAtEpochMs = 2L,
            ),
        )
        val repo = FakeMemoryRepository(readyCount = 3)
        val readiness = LoadMeaningSearchReadiness(
            embeddingEngine = FixedAvailableEngine(model),
            embeddingStore = summaryStore,
            evidenceEmbeddingStore = evidenceStore,
            memoryRepository = repo,
            applyMig05EvidenceSearchCutover = ApplyMig05EvidenceSearchCutover(
                memoryRepository = repo,
                evidenceEmbeddingStore = evidenceStore,
            ),
        )()
        val ready = readiness as MeaningSearchReadiness.Ready
        assertEquals(2, ready.indexedCount)
        assertEquals(3, ready.memoriesReadyCount)
        assertEquals(model, ready.model)
    }

    private class FixedAvailableEngine(
        private val model: ModelVersionIdentity,
    ) : EmbeddingEngine {
        override fun availability(): CapabilityAvailability =
            CapabilityAvailability.Available(model)

        override fun limits(): CapabilityLimits? = null

        override fun embedText(text: String): EmbeddingEncodeResult =
            EmbeddingEncodeResult.Success(EmbeddingVector(floatArrayOf(1f)), model)
    }

    private class FakeMemoryRepository(
        private val readyCount: Int = 0,
    ) : MemoryRepository {
        override suspend fun find(
            assetIdentity: AssetIdentity,
            assetFingerprint: AssetFingerprint,
            assemblySchemaVersion: MemoryAssemblySchemaVersion,
        ): Memory? = null

        override suspend fun insert(memory: Memory): MemoryInsertResult =
            MemoryInsertResult.FailedSafely

        override suspend fun countCurrentReady(): Int = readyCount

        override suspend fun countMeaningIndexCandidates(): Int = readyCount

        override suspend fun listCurrentReadySummaries(limit: Int) =
            emptyList<MemoryEmbeddingSummary>()

        override suspend fun listMeaningIndexSummaries(limit: Int) =
            emptyList<MemoryEmbeddingSummary>()

        override suspend fun listCurrentReadyRevisionIds(): Set<MemoryRevisionId> = emptySet()

        override suspend fun listCurrentStaleReindexRevisionIds(): Set<MemoryRevisionId> =
            emptySet()

        override suspend fun markIntegrityState(
            revisionIds: Collection<MemoryRevisionId>,
            from: MemoryIntegrityState,
            to: MemoryIntegrityState,
            nowEpochMs: Long,
        ): Int = 0

        override suspend fun findCurrentReadyMeaningLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, MemoryMeaningLookup>()

        override suspend fun findPdfPageEvidenceIds(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, Map<Int, MemoryEvidenceId>>()

        override suspend fun findEvidenceSearchRows(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>>()
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
