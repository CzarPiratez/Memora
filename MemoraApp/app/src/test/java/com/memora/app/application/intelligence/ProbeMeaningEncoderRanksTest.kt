package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
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
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.UnavailableEmbeddingEngine
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAnchor
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbeMeaningEncoderRanksTest {
    private val model = ModelVersionIdentity("test-embedder", "1")

    @Test
    fun default_cues_include_paraphrase_and_exact_token_classes() {
        val queries = MeaningEncoderProbeCues.DEFAULT.map { it.query.lowercase() }
        assertTrue(queries.any { it.contains("swimming schedule") })
        assertTrue(queries.any { it.contains("when are the swimming classes") })
        assertTrue(queries.any { it == "passport" || it.contains("aadhaar") })
        assertEquals(17, MeaningEncoderProbeCues.DEFAULT.size)
        assertTrue(queries.any { it.contains("for grade 2") })
    }

    @Test
    fun unavailable_engine_does_not_scan() = runBlocking {
        val report = ProbeMeaningEncoderRanks(
            embeddingEngine = UnavailableEmbeddingEngine("model missing"),
            embeddingStore = InMemoryMemoryEmbeddingStore(),
            evidenceEmbeddingStore = InMemoryMemoryEvidenceEmbeddingStore(),
            memoryRepository = FakeRepo(),
        )()
        assertTrue(report is MeaningEncoderProbeReport.EngineUnavailable)
    }

    @Test
    fun gold_evidence_chunk_rank_is_one_when_it_is_the_nearest() = runBlocking {
        val timetable = MemoryRevisionId("rev-tt")
        val other = MemoryRevisionId("rev-other")
        val summary = InMemoryMemoryEmbeddingStore()
        val evidence = InMemoryMemoryEvidenceEmbeddingStore()
        summary.upsert(record(timetable, MemoryId("m-tt"), floatArrayOf(0.2f, 0.1f)))
        summary.upsert(record(other, MemoryId("m-o"), floatArrayOf(0.1f, 0.9f)))
        evidence.upsert(
            evidenceRecord(timetable, MemoryId("m-tt"), "e-page", floatArrayOf(1f, 0f)),
        )
        val engine = FixedQueryEngine(model, floatArrayOf(1f, 0f))
        val repo = FakeRepo(
            lookups = mapOf(
                timetable to lookup(timetable, "m-tt", "Grade-2-Swimming-TT-2026.pdf"),
                other to lookup(other, "m-o", "unrelated.pdf"),
            ),
            evidenceRows = mapOf(
                timetable to mapOf(
                    MemoryEvidenceId("e-page") to MemoryEvidenceSearchRow(
                        revisionId = timetable,
                        evidenceId = MemoryEvidenceId("e-page"),
                        locator = "pdf:page:1",
                        excerpt = "Grade 2 Swimming Timetable",
                    ),
                ),
            ),
        )
        val report = ProbeMeaningEncoderRanks(engine, summary, evidence, repo)(
            cues = listOf(MeaningEncoderProbeCue("swimming schedule", listOf("timetable", "swimming-tt"))),
            poolLimit = 30,
        ) as MeaningEncoderProbeReport.Completed
        val row = report.rows.single()
        assertEquals(1, row.chunkRank)
        assertEquals(1, row.assetRankAfterCollapse)
        assertEquals(true, row.inCosineSeatedPool)
        assertEquals(true, row.inTokenSeatedPool)
        assertTrue(engine.embedCalls == 1)
    }

    @Test
    fun token_seating_can_drop_a_gold_file_that_cosine_seating_keeps() = runBlocking {
        val gold = MemoryRevisionId("rev-gold")
        val exactA = MemoryRevisionId("rev-exact-a")
        val exactB = MemoryRevisionId("rev-exact-b")
        val summary = InMemoryMemoryEmbeddingStore()
        // Query vector (1,0). Gold is nearest. Exact files are farther but have both words.
        summary.upsert(record(gold, MemoryId("m-g"), floatArrayOf(1f, 0f)))
        summary.upsert(record(exactA, MemoryId("m-a"), floatArrayOf(0.2f, 0.98f)))
        summary.upsert(record(exactB, MemoryId("m-b"), floatArrayOf(0.15f, 0.99f)))
        val engine = FixedQueryEngine(model, floatArrayOf(1f, 0f))
        val repo = FakeRepo(
            lookups = mapOf(
                gold to lookup(gold, "m-g", "Grade-2-Swimming-TT-2026.pdf", "swimming timetable"),
                exactA to lookup(exactA, "m-a", "screenshot-a.png", "swimming schedule heading"),
                exactB to lookup(exactB, "m-b", "screenshot-b.png", "swimming schedule printout"),
            ),
        )
        val report = ProbeMeaningEncoderRanks(engine, summary, InMemoryMemoryEvidenceEmbeddingStore(), repo)(
            cues = listOf(MeaningEncoderProbeCue("swimming schedule", listOf("timetable", "swimming-tt"))),
            poolLimit = 2,
        ) as MeaningEncoderProbeReport.Completed
        val row = report.rows.single()
        assertEquals(1, row.chunkRank)
        assertEquals(1, row.assetRankAfterCollapse)
        assertEquals(false, row.inTokenSeatedPool)
        assertEquals(true, row.inCosineSeatedPool)
    }

    @Test
    fun mark_integrity_is_never_called() = runBlocking {
        val revision = MemoryRevisionId("rev-a")
        val summary = InMemoryMemoryEmbeddingStore()
        summary.upsert(record(revision, MemoryId("m-a"), floatArrayOf(1f, 0f)))
        val repo = object : FakeRepo(
            lookups = mapOf(revision to lookup(revision, "m-a", "file.pdf", "hello")),
        ) {
            override suspend fun markIntegrityState(
                revisionIds: Collection<MemoryRevisionId>,
                from: MemoryIntegrityState,
                to: MemoryIntegrityState,
                nowEpochMs: Long,
            ): Int = error("probe must be read-only")
        }
        val report = ProbeMeaningEncoderRanks(
            FixedQueryEngine(model, floatArrayOf(1f, 0f)),
            summary,
            InMemoryMemoryEvidenceEmbeddingStore(),
            repo,
        )(cues = listOf(MeaningEncoderProbeCue("hello", listOf("file"))))
        assertTrue(report is MeaningEncoderProbeReport.Completed)
    }

    @Test
    fun filler_query_skips_embed() = runBlocking {
        val engine = FixedQueryEngine(model, floatArrayOf(1f, 0f))
        val revision = MemoryRevisionId("rev-a")
        val summary = InMemoryMemoryEmbeddingStore()
        summary.upsert(record(revision, MemoryId("m-a"), floatArrayOf(1f, 0f)))
        val report = ProbeMeaningEncoderRanks(
            engine,
            summary,
            InMemoryMemoryEvidenceEmbeddingStore(),
            FakeRepo(lookups = mapOf(revision to lookup(revision, "m-a", "file.pdf"))),
        )(cues = listOf(MeaningEncoderProbeCue("show me the files"))) as MeaningEncoderProbeReport.Completed
        assertEquals("no content tokens", report.rows.single().skipped)
        assertEquals(0, engine.embedCalls)
    }

    @Test
    fun log_line_does_not_include_excerpt_text() {
        val line = MeaningEncoderProbeRow(
            query = "swimming schedule",
            embedText = "swimming schedule",
            contentTokens = listOf("swimming", "schedule"),
            vectorsScanned = 10,
            goldAssetLabel = "TT.pdf",
            topCollapsedLabels = listOf("TT.pdf"),
        ).logLine()
        assertTrue(line.contains("query="))
        assertFalse(line.contains("PERIOD TIME MON"))
    }

    private fun record(
        revisionId: MemoryRevisionId,
        memoryId: MemoryId,
        values: FloatArray,
    ) = MemoryEmbeddingRecord(
        revisionId = revisionId,
        memoryId = memoryId,
        model = model,
        vector = EmbeddingVector(values),
        sourceTextFingerprint = "fp-${revisionId.value}",
        createdAtEpochMs = 1L,
    )

    private fun evidenceRecord(
        revisionId: MemoryRevisionId,
        memoryId: MemoryId,
        evidenceId: String,
        values: FloatArray,
    ) = MemoryEvidenceEmbeddingRecord(
        revisionId = revisionId,
        memoryId = memoryId,
        evidenceId = MemoryEvidenceId(evidenceId),
        model = model,
        vector = EmbeddingVector(values),
        sourceTextFingerprint = "fp-e-$evidenceId",
        createdAtEpochMs = 1L,
    )

    private fun lookup(
        revisionId: MemoryRevisionId,
        memoryId: String,
        label: String,
        summary: String = "summary",
    ) = MemoryMeaningLookup(
        revisionId = revisionId,
        memoryId = MemoryId(memoryId),
        sourceId = SourceId("saf"),
        sourceAssetKey = SourceAssetKey(label),
        assetType = AssetType.PDF,
        displayLabel = label,
        summaryText = summary,
    )

    private class FixedQueryEngine(
        private val model: ModelVersionIdentity,
        private val query: FloatArray,
    ) : EmbeddingEngine {
        var embedCalls = 0

        override fun availability(): CapabilityAvailability =
            CapabilityAvailability.Available(model)

        override fun limits(): CapabilityLimits =
            CapabilityLimits(maxInputBytes = 1024, maxOutputItems = 1)

        override fun embedText(text: String): EmbeddingEncodeResult {
            embedCalls += 1
            return EmbeddingEncodeResult.Success(EmbeddingVector(query.copyOf()), model)
        }
    }

    private class InMemoryMemoryEmbeddingStore : MemoryEmbeddingStore {
        private val records = linkedMapOf<String, MemoryEmbeddingRecord>()

        private fun key(revisionId: MemoryRevisionId, model: ModelVersionIdentity) =
            "${revisionId.value}|${model.modelId}|${model.version}"

        override fun find(revisionId: MemoryRevisionId, model: ModelVersionIdentity) =
            records[key(revisionId, model)]

        override fun upsert(record: MemoryEmbeddingRecord) {
            records[key(record.revisionId, record.model)] = record
        }

        override fun countForModel(model: ModelVersionIdentity) =
            records.values.count { it.model == model }

        override fun listForModel(model: ModelVersionIdentity) =
            records.values.filter { it.model == model }.toList()

        override fun deleteForModel(model: ModelVersionIdentity): Int {
            val keysToRemove = records.filterValues { it.model == model }.keys.toList()
            keysToRemove.forEach { records.remove(it) }
            return keysToRemove.size
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
        ) = records[key(revisionId, evidenceId, model)]

        override fun upsert(record: MemoryEvidenceEmbeddingRecord) {
            records[key(record.revisionId, record.evidenceId, record.model)] = record
        }

        override fun countForModel(model: ModelVersionIdentity) =
            records.values.count { it.model == model }

        override fun listForModel(model: ModelVersionIdentity) =
            records.values.filter { it.model == model }.toList()

        override fun deleteForModel(model: ModelVersionIdentity): Int {
            val keysToRemove = records.filterValues { it.model == model }.keys.toList()
            keysToRemove.forEach { records.remove(it) }
            return keysToRemove.size
        }
    }

    private open class FakeRepo(
        private val lookups: Map<MemoryRevisionId, MemoryMeaningLookup> = emptyMap(),
        private val evidenceRows: Map<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>> =
            emptyMap(),
    ) : MemoryRepository {
        override suspend fun find(
            assetIdentity: AssetIdentity,
            assetFingerprint: AssetFingerprint,
            assemblySchemaVersion: MemoryAssemblySchemaVersion,
        ): Memory? = null

        override suspend fun insert(memory: Memory) = MemoryInsertResult.FailedSafely

        override suspend fun countCurrentReady() = lookups.size

        override suspend fun countMeaningIndexCandidates() = lookups.size

        override suspend fun listCurrentReadySummaries(limit: Int) = emptyList<MemoryEmbeddingSummary>()

        override suspend fun countMeaningIndexPending(model: ModelVersionIdentity) = 0

        override suspend fun listMeaningIndexSummaries(
            model: ModelVersionIdentity,
            limit: Int,
        ) = emptyList<MemoryEmbeddingSummary>()

        override suspend fun listCurrentReadyRevisionIds() = lookups.keys

        override suspend fun listCurrentStaleReindexRevisionIds() = emptySet<MemoryRevisionId>()

        override suspend fun markIntegrityState(
            revisionIds: Collection<MemoryRevisionId>,
            from: MemoryIntegrityState,
            to: MemoryIntegrityState,
            nowEpochMs: Long,
        ) = 0

        override suspend fun findCurrentReadyMeaningLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ) = lookups.filterKeys { it in revisionIds }

        override suspend fun findMeaningIndexLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ) = lookups.filterKeys { it in revisionIds }

        override suspend fun findPdfPageEvidenceIds(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, Map<Int, MemoryEvidenceId>>()

        override suspend fun findEvidenceSearchRows(
            revisionIds: Collection<MemoryRevisionId>,
        ) = evidenceRows.filterKeys { it in revisionIds }

        override suspend fun findOcrTextEvidenceForEmbedding(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, List<MemoryEvidenceSearchRow>>()

        override suspend fun findNoteTextEvidenceForEmbedding(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, List<MemoryEvidenceSearchRow>>()

        override suspend fun findSignatureAnchors(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, List<MemoryAnchor>>()
    }
}
