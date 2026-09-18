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
import com.memora.app.domain.intelligence.RecallPrecision
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
import org.junit.Assert.assertFalse
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
                    // Both carry the content word `notes`; only the TIME anchor
                    // separates them. Before T10 this passed for the wrong reason —
                    // a time cue used to switch the lexical gate off entirely.
                    revMatch to lookup(revMatch, "match.pdf", "Parent evening notes"),
                    revMiss to lookup(revMiss, "miss.pdf", "Sports day notes"),
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

    /**
     * D-20: Exact must not delete a Partial neighbour, and that neighbour must
     * still be on the list *after* [MeaningTrustedHitPolicy] trim. Cosines are
     * close so the 0.22 band is not the thing under test; token-count pool
     * seating is unchanged.
     */
    @Test
    fun searchByMeaning_keeps_a_timetable_neighbour_after_trusted_trim_when_an_exact_hit_exists() =
        runBlocking {
            val exact = MemoryRevisionId("rev-schedule")
            val timetable = MemoryRevisionId("rev-timetable")
            val engine = FixedEmbeddingEngine(dimensions = 3)
            engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
            val embeddingStore = InMemoryMemoryEmbeddingStore()
            embeddingStore.upsert(record(exact, 0.90f))
            embeddingStore.upsert(record(timetable, 0.88f))
            val recall = recall(
                embeddingEngine = engine,
                embeddingStore = embeddingStore,
                memoryRepository = MeaningLookupRepository(
                    mapOf(
                        exact to lookup(exact, "Sched.pdf", "Swimming schedule term 2"),
                        timetable to lookup(
                            timetable,
                            "Grade-2-Swimming-TT-2026.pdf",
                            "Grade 2 Swimming timetable 2026 PERIOD TIME MON TUE",
                        ),
                    ),
                ),
            )

            val outcome = recall.searchByMeaning("swimming schedule") as MeaningSearchOutcome.Matches

            assertEquals(setOf(exact, timetable), outcome.hits.map { it.revisionId }.toSet())
            assertEquals(
                RecallPrecision.Partial(
                    matched = listOf("swimming"),
                    missing = listOf("schedule"),
                    exactHitsPresent = true,
                ),
                outcome.precision,
            )
        }

    /**
     * MVP page is 20, not 5. Close lexical neighbours must not be truncated
     * just because more than five survived the band.
     */
    @Test
    fun searchByMeaning_does_not_truncate_a_close_band_to_five() = runBlocking {
        val engine = FixedEmbeddingEngine(dimensions = 3)
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val embeddingStore = InMemoryMemoryEmbeddingStore()
        val lookups = (0 until 12).associate { index ->
            val revision = MemoryRevisionId("rev-$index")
            embeddingStore.upsert(record(revision, 0.90f - index * 0.002f))
            revision to lookup(
                revision,
                "file-$index.pdf",
                "Swimming schedule notes $index",
            )
        }
        val recall = recall(
            embeddingEngine = engine,
            embeddingStore = embeddingStore,
            memoryRepository = MeaningLookupRepository(lookups),
        )

        val outcome = recall.searchByMeaning("swimming schedule") as MeaningSearchOutcome.Matches

        assertEquals(12, outcome.hits.size)
        assertTrue(outcome.hits.size > 5)
        assertFalse(outcome.limitReached)
    }

    @Test
    fun searchByMeaning_does_not_claim_limit_reached_when_the_band_is_thin() = runBlocking {
        val engine = FixedEmbeddingEngine(dimensions = 3)
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val embeddingStore = InMemoryMemoryEmbeddingStore()
        val lookups = mutableMapOf<MemoryRevisionId, MemoryMeaningLookup>()
        (0 until 3).forEach { index ->
            val revision = MemoryRevisionId("close-$index")
            embeddingStore.upsert(record(revision, 0.90f - index * 0.002f))
            lookups[revision] = lookup(revision, "close-$index.pdf", "Invoice close $index")
        }
        (0 until 8).forEach { index ->
            val revision = MemoryRevisionId("far-$index")
            embeddingStore.upsert(record(revision, 0.40f))
            lookups[revision] = lookup(revision, "far-$index.pdf", "Invoice far $index")
        }
        val recall = recall(
            embeddingEngine = engine,
            embeddingStore = embeddingStore,
            memoryRepository = MeaningLookupRepository(lookups),
        )

        val outcome = recall.searchByMeaning("invoice") as MeaningSearchOutcome.Matches

        assertEquals(3, outcome.hits.size)
        assertFalse(outcome.limitReached)
    }

    @Test
    fun searchByMeaning_records_pool_counts_and_partial_tier_drops() = runBlocking {
        val scan = MemoryRevisionId("rev-scan")
        val silky = MemoryRevisionId("rev-silky")
        val engine = FixedEmbeddingEngine(dimensions = 3)
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val embeddingStore = InMemoryMemoryEmbeddingStore()
        embeddingStore.upsert(record(scan, 0.90f))
        embeddingStore.upsert(record(silky, 0.88f))
        val recall = recall(
            embeddingEngine = engine,
            embeddingStore = embeddingStore,
            memoryRepository = MeaningLookupRepository(
                mapOf(
                    scan to lookup(scan, "Scan.pdf", "Document scan of the form"),
                    silky to lookup(silky, "Silky.pdf", "silky spelling list"),
                ),
            ),
        )

        val outcome = recall.searchByMeaning("scan silky") as MeaningSearchOutcome.Matches

        assertEquals(listOf(scan), outcome.hits.map { it.revisionId })
        assertEquals(2, outcome.debugTrace?.vectorsScanned)
        assertEquals(2, outcome.debugTrace?.admitted)
        assertEquals(1, outcome.debugTrace?.droppedByTier)
        assertEquals(listOf("Silky.pdf"), outcome.debugTrace?.droppedByTierLabels)
    }

    @Test
    fun searchByMeaning_caps_a_wide_band_at_twenty_and_sets_limit_reached() = runBlocking {
        val engine = FixedEmbeddingEngine(dimensions = 3)
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val embeddingStore = InMemoryMemoryEmbeddingStore()
        val lookups = (0 until 25).associate { index ->
            val revision = MemoryRevisionId("rev-$index")
            embeddingStore.upsert(record(revision, 0.90f - index * 0.001f))
            revision to lookup(
                revision,
                "file-$index.pdf",
                "Invoice notes $index",
            )
        }
        val recall = recall(
            embeddingEngine = engine,
            embeddingStore = embeddingStore,
            memoryRepository = MeaningLookupRepository(lookups),
        )

        val outcome = recall.searchByMeaning("invoice") as MeaningSearchOutcome.Matches

        assertEquals(MeaningTrustedHitPolicy.MAX_TRUSTED_HITS, outcome.hits.size)
        assertTrue(outcome.limitReached)
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
