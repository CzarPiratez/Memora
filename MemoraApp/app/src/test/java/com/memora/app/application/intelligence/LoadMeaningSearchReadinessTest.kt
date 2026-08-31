package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.UnavailableEmbeddingEngine
import com.memora.app.domain.memory.CorpusCompletenessCounts
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadMeaningSearchReadinessTest {
    @Test
    fun unavailable_engine_short_circuits() = runBlocking {
        val readiness = LoadMeaningSearchReadiness(
            embeddingEngine = UnavailableEmbeddingEngine("missing"),
            loadCorpusCompleteness = LoadCorpusCompleteness(
                memoryRepository = com.memora.app.application.memory.EmptyMemoryRepositoryDelegate(),
                factSource = object : com.memora.app.domain.memory.AssetMemoryFactSource {
                    override suspend fun loadCurrentFacts(
                        asset: com.memora.app.domain.asset.Asset,
                    ) = emptyList<com.memora.app.domain.memory.AssetMemoryFact>()

                    override suspend fun findNextPendingAsset(
                        assemblySchemaVersion: com.memora.app.domain.memory.MemoryAssemblySchemaVersion,
                    ) = null

                    override suspend fun countPendingAssembly(
                        assemblySchemaVersion: com.memora.app.domain.memory.MemoryAssemblySchemaVersion,
                    ) = 0
                },
                embeddingEngine = UnavailableEmbeddingEngine("missing"),
                embeddingStore = object : com.memora.app.domain.intelligence.MemoryEmbeddingStore {
                    override fun find(
                        revisionId: com.memora.app.domain.memory.MemoryRevisionId,
                        model: com.memora.app.domain.intelligence.ModelVersionIdentity,
                    ) = null

                    override fun upsert(record: com.memora.app.domain.intelligence.MemoryEmbeddingRecord) = Unit

                    override fun listForModel(model: com.memora.app.domain.intelligence.ModelVersionIdentity) =
                        emptyList<com.memora.app.domain.intelligence.MemoryEmbeddingRecord>()

                    override fun countForModel(model: com.memora.app.domain.intelligence.ModelVersionIdentity) = 0
                },
                evidenceEmbeddingStore = object : com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore {
                    override fun find(
                        revisionId: com.memora.app.domain.memory.MemoryRevisionId,
                        evidenceId: com.memora.app.domain.memory.MemoryEvidenceId,
                        model: com.memora.app.domain.intelligence.ModelVersionIdentity,
                    ) = null

                    override fun upsert(record: com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord) =
                        Unit

                    override fun listForModel(model: com.memora.app.domain.intelligence.ModelVersionIdentity) =
                        emptyList<com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord>()

                    override fun countForModel(model: com.memora.app.domain.intelligence.ModelVersionIdentity) = 0
                },
                applyMig05EvidenceSearchCutover = ApplyMig05EvidenceSearchCutover(
                    memoryRepository = com.memora.app.application.memory.EmptyMemoryRepositoryDelegate(),
                    embeddingStore = object : com.memora.app.domain.intelligence.MemoryEmbeddingStore {
                        override fun find(
                            revisionId: com.memora.app.domain.memory.MemoryRevisionId,
                            model: com.memora.app.domain.intelligence.ModelVersionIdentity,
                        ) = null

                        override fun upsert(record: com.memora.app.domain.intelligence.MemoryEmbeddingRecord) = Unit

                        override fun listForModel(model: com.memora.app.domain.intelligence.ModelVersionIdentity) =
                            emptyList<com.memora.app.domain.intelligence.MemoryEmbeddingRecord>()

                        override fun countForModel(model: com.memora.app.domain.intelligence.ModelVersionIdentity) = 0
                    },
                    evidenceEmbeddingStore = object : com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore {
                        override fun find(
                            revisionId: com.memora.app.domain.memory.MemoryRevisionId,
                            evidenceId: com.memora.app.domain.memory.MemoryEvidenceId,
                            model: com.memora.app.domain.intelligence.ModelVersionIdentity,
                        ) = null

                        override fun upsert(
                            record: com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord,
                        ) = Unit

                        override fun listForModel(
                            model: com.memora.app.domain.intelligence.ModelVersionIdentity,
                        ) = emptyList<com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord>()

                        override fun countForModel(
                            model: com.memora.app.domain.intelligence.ModelVersionIdentity,
                        ) = 0
                    },
                ),
            ),
        )()
        assertTrue(readiness is MeaningSearchReadiness.EngineUnavailable)
    }
}
