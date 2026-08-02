package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.UnavailableEmbeddingEngine
import com.memora.app.domain.memory.MemoryEmbeddingSummary
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryInsertResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAssetMemoriesByMeaningTest {
    private val model = ModelVersionIdentity("test-embedder", "1")

    @Test
    fun unavailable_engine_returns_unavailable_without_ranking() = runBlocking {
        val outcome = SearchAssetMemoriesByMeaning(
            embeddingEngine = UnavailableEmbeddingEngine("model missing"),
            embeddingStore = InMemoryMemoryEmbeddingStore(),
            memoryRepository = FakeMemoryRepository(),
        )("cafe receipt")
        assertTrue(outcome is MeaningSearchOutcome.EngineUnavailable)
    }

    @Test
    fun empty_index_is_distinct_from_no_matches() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 4)
        val outcome = SearchAssetMemoriesByMeaning(
            embeddingEngine = engine,
            embeddingStore = InMemoryMemoryEmbeddingStore(),
            memoryRepository = FakeMemoryRepository(),
        )("cafe receipt")
        assertTrue(outcome is MeaningSearchOutcome.NothingIndexed)
    }

    @Test
    fun ranks_closer_summary_first() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val store = InMemoryMemoryEmbeddingStore()
        val cafeRevision = MemoryRevisionId("rev-cafe")
        val unrelatedRevision = MemoryRevisionId("rev-other")
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = cafeRevision,
                memoryId = MemoryId("mem-cafe"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f)),
                sourceTextFingerprint = "fp-cafe",
                createdAtEpochMs = 1L,
            ),
        )
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = unrelatedRevision,
                memoryId = MemoryId("mem-other"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(0f, 1f, 0f)),
                sourceTextFingerprint = "fp-other",
                createdAtEpochMs = 2L,
            ),
        )
        val repo = FakeMemoryRepository(
            lookups = mapOf(
                cafeRevision to lookup(cafeRevision, MemoryId("mem-cafe"), "Cafe receipt"),
                unrelatedRevision to lookup(
                    unrelatedRevision,
                    MemoryId("mem-other"),
                    "Unrelated note",
                ),
            ),
        )
        // Query embedding aligned with cafe vector.
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(0.9f, 0.1f, 0f))
        val outcome = SearchAssetMemoriesByMeaning(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = repo,
        )("coffee shop bill")
        val matches = outcome as MeaningSearchOutcome.Matches
        assertEquals(2, matches.hits.size)
        assertEquals("Cafe receipt", matches.hits.first().label)
        assertTrue(matches.hits.first().score > matches.hits.last().score)
    }

    private fun lookup(
        revisionId: MemoryRevisionId,
        memoryId: MemoryId,
        label: String,
    ) = MemoryMeaningLookup(
        revisionId = revisionId,
        memoryId = memoryId,
        sourceId = SourceId("source"),
        sourceAssetKey = SourceAssetKey(label),
        assetType = AssetType.PDF,
        displayLabel = label,
        summaryText = "$label summary text for evidence",
    )

    private class FixedEmbeddingEngine(
        private val model: ModelVersionIdentity,
        private val dimensions: Int,
    ) : EmbeddingEngine {
        var nextQueryVector: EmbeddingVector =
            EmbeddingVector(FloatArray(dimensions) { if (it == 0) 1f else 0f })

        override fun availability(): CapabilityAvailability =
            CapabilityAvailability.Available(model)

        override fun limits(): CapabilityLimits =
            CapabilityLimits(maxInputBytes = 1024, maxOutputItems = 1)

        override fun embedText(text: String): EmbeddingEncodeResult =
            EmbeddingEncodeResult.Success(nextQueryVector, model)
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

    private class FakeMemoryRepository(
        private val lookups: Map<MemoryRevisionId, MemoryMeaningLookup> = emptyMap(),
    ) : MemoryRepository {
        override suspend fun find(
            assetIdentity: AssetIdentity,
            assetFingerprint: AssetFingerprint,
            assemblySchemaVersion: MemoryAssemblySchemaVersion,
        ): Memory? = null

        override suspend fun insert(memory: Memory): MemoryInsertResult =
            MemoryInsertResult.FailedSafely

        override suspend fun countCurrentReady(): Int = lookups.size

        override suspend fun listCurrentReadySummaries(limit: Int) =
            emptyList<MemoryEmbeddingSummary>()

        override suspend fun findCurrentReadyMeaningLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, MemoryMeaningLookup> =
            lookups.filterKeys { it in revisionIds }
    }
}
