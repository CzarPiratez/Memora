package com.memora.app.application.intelligence

import com.memora.app.application.memory.EmptyMemoryRepositoryDelegate
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.SavedPdfPageText
import com.memora.app.domain.extraction.SavedPdfPageTextSource
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
import com.memora.app.domain.memory.MemoryEmbeddingSummary
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceSearchRow
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RunPendingMeaningIndexTest {
    @Test
    fun unavailable_engine_writes_nothing() = runTest {
        val store = InMemoryMemoryEmbeddingStore()
        val result = drain(
            engine = UnavailableEmbeddingEngine("missing pack"),
            summaries = listOf(summary("rev-1")),
            embeddingStore = store,
        )(nowEpochMs = 1L)

        assertTrue(result is RunPendingMeaningIndexResult.EngineUnavailable)
        assertEquals(0, store.countForModel(ModelVersionIdentity("x", "1")))
    }

    @Test
    fun empty_queue_is_nothing_pending() = runTest {
        val result = drain(summaries = emptyList())(nowEpochMs = 1L)
        assertEquals(RunPendingMeaningIndexResult.NothingPending, result)
    }

    @Test
    fun pending_count_without_a_selectable_row_is_disagreement_not_empty() = runTest {
        val result = drain(
            summaries = emptyList(),
            pendingOverride = 4,
        )(nowEpochMs = 1L)

        assertTrue(result is RunPendingMeaningIndexResult.SelectionDisagreed)
        assertEquals(4, (result as RunPendingMeaningIndexResult.SelectionDisagreed).pendingCount)
    }

    @Test
    fun one_memory_indexes_and_hasMore_is_false() = runTest {
        val result = drain(summaries = listOf(summary("rev-1")))(nowEpochMs = 1L)

        assertTrue(result is RunPendingMeaningIndexResult.Completed)
        val completed = result as RunPendingMeaningIndexResult.Completed
        assertEquals(1, completed.memories.indexed)
        assertFalse(completed.hasMore)
        assertEquals(0, completed.remainingPending)
    }

    @Test
    fun remaining_work_after_the_batch_sets_hasMore() = runTest {
        val result = drain(
            summaries = listOf(summary("rev-1"), summary("rev-2")),
        )(nowEpochMs = 1L, limit = 1)

        assertTrue(result is RunPendingMeaningIndexResult.Completed)
        val completed = result as RunPendingMeaningIndexResult.Completed
        assertEquals(1, completed.memories.indexed)
        assertTrue(completed.hasMore)
        assertEquals(1, completed.remainingPending)
    }

    @Test
    fun progress_starts_with_summaries() = runTest {
        val progress = mutableListOf<MeaningIndexDrainProgress>()
        drain(summaries = listOf(summary("rev-1")))(
            nowEpochMs = 1L,
            onProgress = { progress += it },
        )

        assertEquals(MeaningIndexDrainPhase.SUMMARIES, progress.first().phase)
        assertEquals(0, progress.first().processed)
        assertTrue(progress.any { it.phase == MeaningIndexDrainPhase.SUMMARIES && it.processed == 1 })
    }

    @Test
    fun pdf_pages_are_indexed_only_for_pdf_memories() = runTest {
        val result = drain(
            summaries = listOf(summary("rev-pdf"), summary("rev-photo")),
            lookups = mapOf(
                MemoryRevisionId("rev-pdf") to lookup("rev-pdf", AssetType.PDF),
                MemoryRevisionId("rev-photo") to lookup("rev-photo", AssetType.PHOTO),
            ),
            pages = mapOf("rev-pdf" to listOf(SavedPdfPageText(1, "Grade 2 swimming"))),
            pdfEvidenceIds = mapOf(
                MemoryRevisionId("rev-pdf") to mapOf(1 to MemoryEvidenceId("e1")),
            ),
        )(nowEpochMs = 1L)

        val completed = result as RunPendingMeaningIndexResult.Completed
        assertTrue(completed.pdf is IndexPdfPageEmbeddingsResult.Completed)
        assertEquals(1, (completed.pdf as IndexPdfPageEmbeddingsResult.Completed).indexed)
    }

    @Test
    fun failed_summaries_still_count_as_remaining() = runTest {
        val result = drain(
            summaries = listOf(summary("rev-1").copy(summaryText = " ")),
        )(nowEpochMs = 1L)

        val completed = result as RunPendingMeaningIndexResult.Completed
        assertEquals(0, completed.memories.indexed)
        assertEquals(1, completed.memories.failed)
        assertTrue(completed.hasMore)
        assertEquals(1, completed.remainingPending)
    }

    @Test
    fun empty_queue_does_not_run_cutover() = runTest {
        var readyListCalls = 0
        drain(
            summaries = emptyList(),
            onReadyListCalled = { readyListCalls += 1 },
        )(nowEpochMs = 1L)
        assertEquals(0, readyListCalls)
    }

    @Test
    fun selection_disagreement_does_not_run_cutover() = runTest {
        var readyListCalls = 0
        drain(
            summaries = emptyList(),
            pendingOverride = 3,
            onReadyListCalled = { readyListCalls += 1 },
        )(nowEpochMs = 1L)
        assertEquals(0, readyListCalls)
    }

    @Test
    fun completed_batch_consults_cutover_selection() = runTest {
        var readyListCalls = 0
        drain(
            summaries = listOf(summary("rev-1")),
            onReadyListCalled = { readyListCalls += 1 },
        )(nowEpochMs = 1L)
        assertTrue(readyListCalls > 0)
    }

    @Test
    fun ocr_evidence_is_indexed_only_for_photo_or_screenshot() = runTest {
        val result = drain(
            summaries = listOf(summary("rev-photo"), summary("rev-pdf")),
            lookups = mapOf(
                MemoryRevisionId("rev-photo") to lookup("rev-photo", AssetType.PHOTO),
                MemoryRevisionId("rev-pdf") to lookup("rev-pdf", AssetType.PDF),
            ),
            ocrEvidence = mapOf(
                MemoryRevisionId("rev-photo") to listOf(
                    MemoryEvidenceSearchRow(
                        revisionId = MemoryRevisionId("rev-photo"),
                        evidenceId = MemoryEvidenceId("ocr-1"),
                        locator = "ocr:block:1",
                        excerpt = "Grade 2 swimming",
                    ),
                ),
                MemoryRevisionId("rev-pdf") to listOf(
                    MemoryEvidenceSearchRow(
                        revisionId = MemoryRevisionId("rev-pdf"),
                        evidenceId = MemoryEvidenceId("ocr-ignored"),
                        locator = "ocr:block:1",
                        excerpt = "should not index on a PDF",
                    ),
                ),
            ),
        )(nowEpochMs = 1L)

        val completed = result as RunPendingMeaningIndexResult.Completed
        assertTrue(completed.ocr is IndexOcrEvidenceEmbeddingsResult.Completed)
        assertEquals(1, (completed.ocr as IndexOcrEvidenceEmbeddingsResult.Completed).indexed)
    }

    @Test
    fun note_evidence_is_indexed_only_for_notes() = runTest {
        val result = drain(
            summaries = listOf(summary("rev-note"), summary("rev-photo")),
            lookups = mapOf(
                MemoryRevisionId("rev-note") to lookup("rev-note", AssetType.NOTE),
                MemoryRevisionId("rev-photo") to lookup("rev-photo", AssetType.PHOTO),
            ),
            noteEvidence = mapOf(
                MemoryRevisionId("rev-note") to listOf(
                    MemoryEvidenceSearchRow(
                        revisionId = MemoryRevisionId("rev-note"),
                        evidenceId = MemoryEvidenceId("note-1"),
                        locator = "note:body",
                        excerpt = "swimming on Thursday",
                    ),
                ),
            ),
        )(nowEpochMs = 1L)

        val completed = result as RunPendingMeaningIndexResult.Completed
        assertTrue(completed.note is IndexOcrEvidenceEmbeddingsResult.Completed)
        assertEquals(1, (completed.note as IndexOcrEvidenceEmbeddingsResult.Completed).indexed)
    }

    @Test
    fun completed_hasMore_cannot_disagree_with_remaining() {
        assertThrows(IllegalArgumentException::class.java) {
            RunPendingMeaningIndexResult.Completed(
                memories = IndexMemoryEmbeddingsResult.Completed(1, 0, 0),
                pdf = IndexPdfPageEmbeddingsResult.Completed(0, 0, 0),
                ocr = IndexOcrEvidenceEmbeddingsResult.Completed(0, 0, 0),
                note = IndexOcrEvidenceEmbeddingsResult.Completed(0, 0, 0),
                remainingPending = 2,
                hasMore = false,
            )
        }
    }

    private fun drain(
        engine: EmbeddingEngine = FixedDimensionEmbeddingEngine(),
        summaries: List<MemoryEmbeddingSummary>,
        pendingOverride: Int? = null,
        lookups: Map<MemoryRevisionId, MemoryMeaningLookup> = emptyMap(),
        pages: Map<String, List<SavedPdfPageText>> = emptyMap(),
        pdfEvidenceIds: Map<MemoryRevisionId, Map<Int, MemoryEvidenceId>> = emptyMap(),
        ocrEvidence: Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>> = emptyMap(),
        noteEvidence: Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>> = emptyMap(),
        embeddingStore: InMemoryMemoryEmbeddingStore = InMemoryMemoryEmbeddingStore(),
        onReadyListCalled: (() -> Unit)? = null,
    ): RunPendingMeaningIndex {
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val repo = FakeMeaningIndexRepository(
            allSummaries = summaries,
            pendingOverride = pendingOverride,
            embeddingStore = embeddingStore,
            modelProvider = {
                when (val availability = engine.availability()) {
                    is CapabilityAvailability.Available -> availability.model
                    is CapabilityAvailability.Unavailable -> ModelVersionIdentity("none", "none")
                }
            },
            lookups = lookups,
            pdfEvidenceIds = pdfEvidenceIds,
            ocrEvidence = ocrEvidence,
            noteEvidence = noteEvidence,
            onReadyListCalled = onReadyListCalled,
        )
        return RunPendingMeaningIndex(
            embeddingEngine = engine,
            memoryRepository = repo,
            indexMemoryEmbeddings = IndexMemoryEmbeddings(engine, embeddingStore),
            indexPdfPageEmbeddings = IndexPdfPageEmbeddings(engine, evidenceStore),
            indexOcrEvidenceEmbeddings = IndexOcrEvidenceEmbeddings(engine, evidenceStore),
            savedPdfPages = SavedPdfPageTextSource { sourceId, sourceAssetKey ->
                pages[sourceAssetKey].orEmpty().also { require(sourceId.isNotBlank()) }
            },
            applyMig05EvidenceSearchCutover = ApplyMig05EvidenceSearchCutover(
                memoryRepository = repo,
                embeddingStore = embeddingStore,
                evidenceEmbeddingStore = evidenceStore,
            ),
        )
    }

    private fun summary(revision: String) = MemoryEmbeddingSummary(
        revisionId = MemoryRevisionId(revision),
        memoryId = MemoryId("mem-$revision"),
        summaryText = "summary $revision",
    )

    private fun lookup(revision: String, type: AssetType) = MemoryMeaningLookup(
        revisionId = MemoryRevisionId(revision),
        memoryId = MemoryId("mem-$revision"),
        sourceId = SourceId("source"),
        sourceAssetKey = SourceAssetKey(revision),
        assetType = type,
        displayLabel = "file $revision",
        summaryText = "summary $revision",
    )
}

private class FakeMeaningIndexRepository(
    private val allSummaries: List<MemoryEmbeddingSummary>,
    private val pendingOverride: Int?,
    private val embeddingStore: MemoryEmbeddingStore,
    private val modelProvider: () -> ModelVersionIdentity,
    private val lookups: Map<MemoryRevisionId, MemoryMeaningLookup>,
    private val pdfEvidenceIds: Map<MemoryRevisionId, Map<Int, MemoryEvidenceId>>,
    private val ocrEvidence: Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>> = emptyMap(),
    private val noteEvidence: Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>> = emptyMap(),
    private val onReadyListCalled: (() -> Unit)? = null,
) : MemoryRepository by EmptyMemoryRepositoryDelegate() {
    private fun stillPending(): List<MemoryEmbeddingSummary> {
        val model = modelProvider()
        return allSummaries.filter { embeddingStore.find(it.revisionId, model) == null }
    }

    override suspend fun countMeaningIndexPending(model: ModelVersionIdentity): Int =
        pendingOverride ?: stillPending().size

    override suspend fun listMeaningIndexSummaries(
        model: ModelVersionIdentity,
        limit: Int,
    ): List<MemoryEmbeddingSummary> = stillPending().take(limit)

    override suspend fun findMeaningIndexLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ) = lookups.filterKeys { it in revisionIds.toSet() }

    override suspend fun findPdfPageEvidenceIds(
        revisionIds: Collection<MemoryRevisionId>,
    ) = pdfEvidenceIds.filterKeys { it in revisionIds.toSet() }

    override suspend fun findOcrTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ) = ocrEvidence.filterKeys { it in revisionIds.toSet() }

    override suspend fun findNoteTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ) = noteEvidence.filterKeys { it in revisionIds.toSet() }

    override suspend fun listCurrentReadyRevisionIds(): Set<MemoryRevisionId> {
        onReadyListCalled?.invoke()
        return emptySet()
    }

    override suspend fun listCurrentStaleReindexRevisionIds() = emptySet<MemoryRevisionId>()

    override suspend fun markIntegrityState(
        revisionIds: Collection<MemoryRevisionId>,
        from: com.memora.app.domain.memory.MemoryIntegrityState,
        to: com.memora.app.domain.memory.MemoryIntegrityState,
        nowEpochMs: Long,
    ): Int = 0
}

private class FixedDimensionEmbeddingEngine(
    private val model: ModelVersionIdentity =
        ModelVersionIdentity(modelId = "fixed-embed-test", version = "0.0.1"),
) : EmbeddingEngine {
    override fun availability(): CapabilityAvailability = CapabilityAvailability.Available(model)

    override fun limits(): CapabilityLimits? = null

    override fun embedText(text: String): EmbeddingEncodeResult {
        val values = FloatArray(4) { index ->
            ((text.hashCode() + index * 31) % 1000) / 1000f
        }
        return EmbeddingEncodeResult.Success(EmbeddingVector(values), model)
    }
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
