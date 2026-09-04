package com.memora.app.application.memory

import com.memora.app.application.intelligence.ApplyMig05EvidenceSearchCutover
import com.memora.app.application.intelligence.MeaningSearchOutcome
import com.memora.app.application.intelligence.SearchAssetMemoriesByMeaning
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.IdentityRecallRanker
import com.memora.app.domain.intelligence.MemoryEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryAnchor
import com.memora.app.domain.memory.MemoryAnchorId
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceSearchRow
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryIntegrityState
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.MemoryText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Canonical Recall meaning path wiring (MIG-07B). */
class CanonicalRecallMeaningTest {
    private val model = ModelVersionIdentity("test-model", "1")

    @Test
    fun searchByMeaning_surfaces_failed_from_candidate_search() = runBlocking {
        val revision = MemoryRevisionId("rev-fail")
        val embeddingStore = InMemoryMemoryEmbeddingStore()
        embeddingStore.upsert(record(revision, 0.9f))
        val engine = FixedEmbeddingEngine(dimensions = 3)
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val recall = recall(
            embeddingEngine = engine,
            embeddingStore = embeddingStore,
            memoryRepository = ThrowingMeaningLookupRepository(),
        )

        val outcome = recall.searchByMeaning("invoice")

        assertTrue(outcome is MeaningSearchOutcome.Failed)
        assertEquals("lookup failed", (outcome as MeaningSearchOutcome.Failed).reason)
    }

    @Test
    fun searchByMeaning_trims_results_to_requested_limit() = runBlocking {
        val first = MemoryRevisionId("rev-1")
        val second = MemoryRevisionId("rev-2")
        val engine = FixedEmbeddingEngine(dimensions = 3)
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val embeddingStore = InMemoryMemoryEmbeddingStore()
        embeddingStore.upsert(record(first, 0.9f))
        embeddingStore.upsert(record(second, 0.8f))
        val recall = recall(
            embeddingEngine = engine,
            embeddingStore = embeddingStore,
            memoryRepository = MeaningLookupRepository(
                mapOf(
                    // Both must carry the query word: trimming is what is under
                    // test here, not the MF-1 lexical precision gate.
                    first to lookup(first, "first.pdf", "Invoice from the plumber"),
                    second to lookup(second, "second.pdf", "Invoice from the electrician"),
                ),
            ),
        )

        val outcome = recall.searchByMeaning(rawQuery = "invoice", limit = 1)
            as MeaningSearchOutcome.Matches

        assertEquals(1, outcome.hits.size)
        assertEquals("first.pdf", outcome.hits.single().label)
        assertTrue(outcome.limitReached)
    }

    @Test
    fun searchByMeaning_wires_anchor_ranking_for_explicit_time_query() = runBlocking {
        val revMatch = MemoryRevisionId("rev-match")
        val revMiss = MemoryRevisionId("rev-miss")
        val engine = FixedEmbeddingEngine(dimensions = 3)
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val embeddingStore = InMemoryMemoryEmbeddingStore()
        embeddingStore.upsert(
            MemoryEmbeddingRecord(
                revisionId = revMiss,
                memoryId = MemoryId("mem-miss"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f)),
                sourceTextFingerprint = "fp-miss",
                createdAtEpochMs = 1L,
            ),
        )
        embeddingStore.upsert(
            MemoryEmbeddingRecord(
                revisionId = revMatch,
                memoryId = MemoryId("mem-match"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(0.1f, 0.9f, 0f)),
                sourceTextFingerprint = "fp-match",
                createdAtEpochMs = 2L,
            ),
        )
        val recall = recall(
            embeddingEngine = engine,
            embeddingStore = embeddingStore,
            memoryRepository = MeaningLookupRepository(
                lookups = mapOf(
                    revMatch to lookup(revMatch, "match.pdf"),
                    revMiss to lookup(revMiss, "miss.pdf"),
                ),
                anchors = mapOf(
                    revMatch to listOf(
                        timeAnchor("Date taken: 2024-03-01", MemoryEvidenceId("e-time-match")),
                    ),
                    revMiss to listOf(
                        timeAnchor("Date taken: 2022-01-01", MemoryEvidenceId("e-time-miss")),
                    ),
                ),
            ),
        )

        val outcome = recall.searchByMeaning("notes in 2024") as MeaningSearchOutcome.Matches

        assertEquals(1, outcome.hits.size)
        assertEquals(revMatch, outcome.hits.single().revisionId)
    }

    private fun recall(
        embeddingEngine: EmbeddingEngine = UnavailableMeaningEngine(),
        embeddingStore: MemoryEmbeddingStore = InMemoryMemoryEmbeddingStore(),
        evidenceStore: MemoryEvidenceEmbeddingStore = InMemoryMemoryEvidenceEmbeddingStore(),
        memoryRepository: MemoryRepository = EmptyMemoryRepositoryDelegate(),
    ): CanonicalRecall = CanonicalRecall(
        searchMemoryEvidence = SearchMemoryEvidence(
            excerptSearch = EmptyExcerptSearch(),
        ),
        searchAssetMemoriesByMeaning = SearchAssetMemoriesByMeaning(
            embeddingEngine = embeddingEngine,
            embeddingStore = embeddingStore,
            evidenceEmbeddingStore = evidenceStore,
            memoryRepository = memoryRepository,
            applyMig05EvidenceSearchCutover = ApplyMig05EvidenceSearchCutover(
                memoryRepository = memoryRepository,
                embeddingStore = embeddingStore,
                evidenceEmbeddingStore = evidenceStore,
            ),
        ),
        memoryRepository = memoryRepository,
        recallRanker = IdentityRecallRanker,
    )

    private fun record(revisionId: MemoryRevisionId, lead: Float) = MemoryEmbeddingRecord(
        revisionId = revisionId,
        memoryId = MemoryId("mem-${revisionId.value}"),
        model = model,
        vector = EmbeddingVector(floatArrayOf(lead, 1f - lead, 0f)),
        sourceTextFingerprint = "fp-${revisionId.value}",
        createdAtEpochMs = 1L,
    )

    private fun lookup(
        revisionId: MemoryRevisionId,
        label: String,
        summaryText: String = "$label summary for meaning search",
    ) = MemoryMeaningLookup(
        revisionId = revisionId,
        memoryId = MemoryId("mem-${revisionId.value}"),
        sourceId = SourceId("src"),
        sourceAssetKey = SourceAssetKey(label),
        assetType = AssetType.PDF,
        displayLabel = label,
        summaryText = summaryText,
    )

    private fun topicAnchor(text: String, evidenceId: MemoryEvidenceId) = MemoryAnchor(
        id = MemoryAnchorId("topic-$text"),
        kind = MemoryAnchorKind.TOPIC,
        text = MemoryText(text),
        evidenceIds = setOf(evidenceId),
    )

    private fun timeAnchor(text: String, evidenceId: MemoryEvidenceId) = MemoryAnchor(
        id = MemoryAnchorId("time-$text"),
        kind = MemoryAnchorKind.TIME,
        text = MemoryText(text),
        evidenceIds = setOf(evidenceId),
    )

    private class FixedEmbeddingEngine(
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

        companion object {
            private val model = ModelVersionIdentity("test-model", "1")
        }
    }

    private class UnavailableMeaningEngine : EmbeddingEngine {
        override fun availability(): CapabilityAvailability =
            CapabilityAvailability.Unavailable("off")

        override fun limits(): CapabilityLimits? = null

        override fun embedText(text: String): EmbeddingEncodeResult =
            EmbeddingEncodeResult.Unavailable("off")
    }

    private class InMemoryMemoryEmbeddingStore : MemoryEmbeddingStore {
        private val records = linkedMapOf<String, MemoryEmbeddingRecord>()

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

        private fun key(revisionId: MemoryRevisionId, model: ModelVersionIdentity) =
            "${revisionId.value}|${model.modelId}|${model.version}"
    }

    private class InMemoryMemoryEvidenceEmbeddingStore : MemoryEvidenceEmbeddingStore {
        override fun find(
            revisionId: MemoryRevisionId,
            evidenceId: MemoryEvidenceId,
            model: ModelVersionIdentity,
        ) = null

        override fun upsert(record: MemoryEvidenceEmbeddingRecord) = Unit

        override fun countForModel(model: ModelVersionIdentity) = 0

        override fun listForModel(model: ModelVersionIdentity) =
            emptyList<MemoryEvidenceEmbeddingRecord>()
    }

    private open class MeaningSearchMemoryRepositoryBase :
        MemoryRepository by EmptyMemoryRepositoryDelegate() {
        override suspend fun listCurrentReadyRevisionIds(): Set<MemoryRevisionId> = emptySet()

        override suspend fun listCurrentStaleReindexRevisionIds(): Set<MemoryRevisionId> =
            emptySet()

        override suspend fun markIntegrityState(
            revisionIds: Collection<MemoryRevisionId>,
            from: MemoryIntegrityState,
            to: MemoryIntegrityState,
            nowEpochMs: Long,
        ): Int = 0

        override suspend fun findPdfPageEvidenceIds(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, Map<Int, MemoryEvidenceId>> = emptyMap()

        override suspend fun findEvidenceSearchRows(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>> = emptyMap()

        override suspend fun findMeaningIndexLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, MemoryMeaningLookup> = emptyMap()
    }

    private class MeaningLookupRepository(
        private val lookups: Map<MemoryRevisionId, MemoryMeaningLookup> = emptyMap(),
        private val anchors: Map<MemoryRevisionId, List<MemoryAnchor>> = emptyMap(),
    ) : MeaningSearchMemoryRepositoryBase() {
        override suspend fun findMeaningIndexLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, MemoryMeaningLookup> =
            lookups.filterKeys { it in revisionIds }

        override suspend fun findSignatureAnchors(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, List<MemoryAnchor>> =
            anchors.filterKeys { it in revisionIds }
    }

    private class ThrowingMeaningLookupRepository : MeaningSearchMemoryRepositoryBase() {
        override suspend fun findMeaningIndexLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, MemoryMeaningLookup> =
            throw IllegalStateException("lookup failed")
    }

    private class EmptyExcerptSearch : MemoryEvidenceExcerptSearch {
        override suspend fun countCurrentReadyEvidence(
            assemblySchemaVersion: String,
            assetType: AssetType?,
        ): Int = 0

        override suspend fun countCurrentReadyEvidenceCorpus(
            assemblySchemaVersion: String,
            assetType: AssetType?,
        ): MemoryEvidenceCorpusCounts =
            MemoryEvidenceCorpusCounts(evidenceCount = 0, documentCount = 0)

        override suspend fun searchByExcerpt(
            escapedNeedle: String,
            assemblySchemaVersion: String,
            limit: Int,
            assetType: AssetType?,
        ): List<MemoryEvidenceExcerptMatch> = emptyList()
    }
}
