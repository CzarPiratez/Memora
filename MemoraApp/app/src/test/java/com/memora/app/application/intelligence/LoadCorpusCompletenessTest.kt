package com.memora.app.application.intelligence

import com.memora.app.application.memory.AssembleAssetMemoryFromExtractionFacts
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
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.CorpusCompletenessBlocked
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadCorpusCompletenessTest {
    private val model = ModelVersionIdentity("test-embedder", "1")

    @Test
    fun pending_meaning_index_is_candidates_minus_summary_indexed() = runBlocking {
        val summaryStore = InMemoryMemoryEmbeddingStore()
        summaryStore.upsert(
            MemoryEmbeddingRecord(
                revisionId = com.memora.app.domain.memory.MemoryRevisionId("rev-1"),
                memoryId = com.memora.app.domain.memory.MemoryId("mem-1"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f)),
                sourceTextFingerprint = "fp",
                createdAtEpochMs = 1L,
            ),
        )
        val snapshot = loadCorpusCompleteness(
            memoryRepository = FakeMemoryRepository(ready = 5, candidates = 8),
            factSource = FakeFactSource(pendingAssembly = 2),
            embeddingEngine = FixedAvailableEngine(model),
            summaryStore = summaryStore,
            evidenceStore = InMemoryMemoryEvidenceEmbeddingStore(),
        )()

        assertEquals(5, snapshot.counts.memoriesReady)
        assertEquals(2, snapshot.counts.memoriesPendingAssembly)
        assertEquals(1, snapshot.counts.meaningSummaryIndexed)
        assertEquals(0, snapshot.counts.meaningEvidenceIndexed)
        assertEquals(7, snapshot.counts.meaningIndexPending)
        assertNull(snapshot.blocked)
    }

    @Test
    fun blocked_when_memories_pending_assembly_and_none_ready() = runBlocking {
        val snapshot = loadCorpusCompleteness(
            memoryRepository = FakeMemoryRepository(ready = 0, candidates = 0),
            factSource = FakeFactSource(pendingAssembly = 3),
        )()

        assertEquals(CorpusCompletenessBlocked.BuildMemoriesFirst, snapshot.blocked)
    }

    @Test
    fun blocked_when_memories_ready_but_index_empty() = runBlocking {
        val snapshot = loadCorpusCompleteness(
            memoryRepository = FakeMemoryRepository(ready = 2, candidates = 2),
            factSource = FakeFactSource(pendingAssembly = 0),
            embeddingEngine = FixedAvailableEngine(model),
        )()

        assertEquals(CorpusCompletenessBlocked.BuildMeaningIndex, snapshot.blocked)
    }

    @Test
    fun meaning_readiness_wraps_corpus_snapshot() = runBlocking {
        val summaryStore = InMemoryMemoryEmbeddingStore()
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val revision = com.memora.app.domain.memory.MemoryRevisionId("rev-1")
        summaryStore.upsert(
            MemoryEmbeddingRecord(
                revisionId = revision,
                memoryId = com.memora.app.domain.memory.MemoryId("mem-1"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f)),
                sourceTextFingerprint = "fp-s",
                createdAtEpochMs = 1L,
            ),
        )
        evidenceStore.upsert(
            MemoryEvidenceEmbeddingRecord(
                revisionId = revision,
                memoryId = com.memora.app.domain.memory.MemoryId("mem-1"),
                evidenceId = com.memora.app.domain.memory.MemoryEvidenceId("e1"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(0f, 1f)),
                sourceTextFingerprint = "fp-e",
                createdAtEpochMs = 2L,
            ),
        )
        val repo = FakeMemoryRepository(ready = 3, candidates = 3)
        val readiness = LoadMeaningSearchReadiness(
            embeddingEngine = FixedAvailableEngine(model),
            loadCorpusCompleteness = loadCorpusCompleteness(
                memoryRepository = repo,
                factSource = FakeFactSource(),
                embeddingEngine = FixedAvailableEngine(model),
                summaryStore = summaryStore,
                evidenceStore = evidenceStore,
            ),
        )()
        val ready = readiness as MeaningSearchReadiness.Ready
        assertEquals(2, ready.indexedCount)
        assertEquals(3, ready.memoriesReadyCount)
        assertTrue(ready.corpusCompleteness.counts.meaningVectorsIndexed == 2)
    }

    private fun loadCorpusCompleteness(
        memoryRepository: MemoryRepository,
        factSource: AssetMemoryFactSource,
        embeddingEngine: EmbeddingEngine = UnavailableEmbeddingEngine("off"),
        summaryStore: MemoryEmbeddingStore = InMemoryMemoryEmbeddingStore(),
        evidenceStore: MemoryEvidenceEmbeddingStore = InMemoryMemoryEvidenceEmbeddingStore(),
    ) = LoadCorpusCompleteness(
        memoryRepository = memoryRepository,
        factSource = factSource,
        embeddingEngine = embeddingEngine,
        embeddingStore = summaryStore,
        evidenceEmbeddingStore = evidenceStore,
        applyMig05EvidenceSearchCutover = ApplyMig05EvidenceSearchCutover(
            memoryRepository = memoryRepository,
            evidenceEmbeddingStore = evidenceStore,
        ),
    )

    private class FakeFactSource(
        private val pendingAssembly: Int = 0,
    ) : AssetMemoryFactSource {
        override suspend fun loadCurrentFacts(
            asset: com.memora.app.domain.asset.Asset,
        ) = emptyList<com.memora.app.domain.memory.AssetMemoryFact>()

        override suspend fun findNextPendingAsset(
            assemblySchemaVersion: MemoryAssemblySchemaVersion,
        ) = null

        override suspend fun countPendingAssembly(
            assemblySchemaVersion: MemoryAssemblySchemaVersion,
        ): Int = pendingAssembly
    }

    private class FakeMemoryRepository(
        private val ready: Int,
        private val candidates: Int = ready,
    ) : MemoryRepository {
        override suspend fun countCurrentReady(): Int = ready

        override suspend fun countMeaningIndexCandidates(): Int = candidates

        override suspend fun find(
            assetIdentity: com.memora.app.domain.asset.AssetIdentity,
            assetFingerprint: com.memora.app.domain.asset.AssetFingerprint,
            assemblySchemaVersion: com.memora.app.domain.memory.MemoryAssemblySchemaVersion,
        ) = null

        override suspend fun insert(memory: com.memora.app.domain.memory.Memory) =
            com.memora.app.domain.memory.MemoryInsertResult.FailedSafely

        override suspend fun listCurrentReadySummaries(limit: Int) =
            emptyList<com.memora.app.domain.memory.MemoryEmbeddingSummary>()

        override suspend fun listMeaningIndexSummaries(limit: Int) =
            emptyList<com.memora.app.domain.memory.MemoryEmbeddingSummary>()

        override suspend fun listCurrentReadyRevisionIds() =
            emptySet<com.memora.app.domain.memory.MemoryRevisionId>()

        override suspend fun listCurrentStaleReindexRevisionIds() =
            emptySet<com.memora.app.domain.memory.MemoryRevisionId>()

        override suspend fun markIntegrityState(
            revisionIds: Collection<com.memora.app.domain.memory.MemoryRevisionId>,
            from: com.memora.app.domain.memory.MemoryIntegrityState,
            to: com.memora.app.domain.memory.MemoryIntegrityState,
            nowEpochMs: Long,
        ) = 0

        override suspend fun findCurrentReadyMeaningLookups(
            revisionIds: Collection<com.memora.app.domain.memory.MemoryRevisionId>,
        ) = emptyMap<com.memora.app.domain.memory.MemoryRevisionId, com.memora.app.domain.memory.MemoryMeaningLookup>()

        override suspend fun findPdfPageEvidenceIds(
            revisionIds: Collection<com.memora.app.domain.memory.MemoryRevisionId>,
        ) = emptyMap<com.memora.app.domain.memory.MemoryRevisionId, Map<Int, com.memora.app.domain.memory.MemoryEvidenceId>>()

        override suspend fun findEvidenceSearchRows(
            revisionIds: Collection<com.memora.app.domain.memory.MemoryRevisionId>,
        ) = emptyMap<com.memora.app.domain.memory.MemoryRevisionId, Map<com.memora.app.domain.memory.MemoryEvidenceId, com.memora.app.domain.memory.MemoryEvidenceSearchRow>>()

        override suspend fun findOcrTextEvidenceForEmbedding(
            revisionIds: Collection<com.memora.app.domain.memory.MemoryRevisionId>,
        ) = emptyMap<com.memora.app.domain.memory.MemoryRevisionId, List<com.memora.app.domain.memory.MemoryEvidenceSearchRow>>()

        override suspend fun findSignatureAnchors(
            revisionIds: Collection<com.memora.app.domain.memory.MemoryRevisionId>,
        ) = emptyMap<com.memora.app.domain.memory.MemoryRevisionId, List<com.memora.app.domain.memory.MemoryAnchor>>()
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

    private class InMemoryMemoryEmbeddingStore : MemoryEmbeddingStore {
        private val records = linkedMapOf<String, MemoryEmbeddingRecord>()

        private fun key(revisionId: com.memora.app.domain.memory.MemoryRevisionId, model: ModelVersionIdentity) =
            "${revisionId.value}|${model.modelId}|${model.version}"

        override fun find(
            revisionId: com.memora.app.domain.memory.MemoryRevisionId,
            model: ModelVersionIdentity,
        ): MemoryEmbeddingRecord? = records[key(revisionId, model)]

        override fun upsert(record: MemoryEmbeddingRecord) {
            records[key(record.revisionId, record.model)] = record
        }

        override fun countForModel(model: ModelVersionIdentity): Int =
            records.values.count {
                it.model.modelId == model.modelId && it.model.version == model.version
            }

        override fun listForModel(model: ModelVersionIdentity) =
            records.values.filter {
                it.model.modelId == model.modelId && it.model.version == model.version
            }
    }

    private class InMemoryMemoryEvidenceEmbeddingStore : MemoryEvidenceEmbeddingStore {
        private val records = linkedMapOf<String, MemoryEvidenceEmbeddingRecord>()

        private fun key(
            revisionId: com.memora.app.domain.memory.MemoryRevisionId,
            evidenceId: com.memora.app.domain.memory.MemoryEvidenceId,
            model: ModelVersionIdentity,
        ) = "${revisionId.value}|${evidenceId.value}|${model.modelId}|${model.version}"

        override fun find(
            revisionId: com.memora.app.domain.memory.MemoryRevisionId,
            evidenceId: com.memora.app.domain.memory.MemoryEvidenceId,
            model: ModelVersionIdentity,
        ): MemoryEvidenceEmbeddingRecord? = records[key(revisionId, evidenceId, model)]

        override fun upsert(record: MemoryEvidenceEmbeddingRecord) {
            records[key(record.revisionId, record.evidenceId, record.model)] = record
        }

        override fun countForModel(model: ModelVersionIdentity): Int =
            records.values.count {
                it.model.modelId == model.modelId && it.model.version == model.version
            }

        override fun listForModel(model: ModelVersionIdentity) =
            records.values.filter {
                it.model.modelId == model.modelId && it.model.version == model.version
            }
    }
}
