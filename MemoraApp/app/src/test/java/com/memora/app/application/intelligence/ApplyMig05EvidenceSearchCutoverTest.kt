package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
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

class ApplyMig05EvidenceSearchCutoverTest {
    private val model = ModelVersionIdentity("cutover-model", "1")

    @Test
    fun marks_only_gap_revisions_stale_and_restores_when_evidence_present() = runBlocking {
        val gap = MemoryRevisionId("rev-gap")
        val filled = MemoryRevisionId("rev-filled")
        val unrelated = MemoryRevisionId("rev-unrelated")
        val freshNeverIndexed = MemoryRevisionId("rev-fresh")
        val repo = RecordingMemoryRepository(
            ready = mutableSetOf(gap, filled, unrelated, freshNeverIndexed),
            stale = mutableSetOf(),
            pdfPageEvidenceByRevision = mapOf(
                gap to mapOf(1 to MemoryEvidenceId("e1")),
                filled to mapOf(2 to MemoryEvidenceId("e2")),
                freshNeverIndexed to mapOf(1 to MemoryEvidenceId("e-fresh")),
                // unrelated has no pdf:page evidence
            ),
        )
        val summaryStore = InMemoryMemoryEmbeddingStore()
        summaryStore.upsert(summaryRecord(gap))
        summaryStore.upsert(summaryRecord(filled))
        // freshNeverIndexed has page evidence but no summary embedding → not a gap.
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        evidenceStore.upsert(evidenceRecord(filled, MemoryEvidenceId("e2")))

        val cutover = ApplyMig05EvidenceSearchCutover(repo, summaryStore, evidenceStore)
        val first = cutover.ensureApplied(model, nowEpochMs = 10L)
        assertEquals(setOf(gap), first.gapRevisionIds)
        assertEquals(1, first.markedStale)
        assertTrue(gap in repo.stale)
        assertTrue(gap !in repo.ready)
        assertTrue(filled in repo.ready)
        assertTrue(unrelated in repo.ready)
        assertTrue(freshNeverIndexed in repo.ready)

        // Evidence index fills the gap; next ensureApplied restores READY.
        evidenceStore.upsert(evidenceRecord(gap, MemoryEvidenceId("e1")))
        val second = cutover.ensureApplied(model, nowEpochMs = 20L)
        assertTrue(second.gapRevisionIds.isEmpty())
        assertEquals(0, second.markedStale)
        assertEquals(1, second.restoredReady)
        assertTrue(gap in repo.ready)
        assertTrue(gap !in repo.stale)
    }

    @Test
    fun does_not_stale_fresh_pdf_before_summary_index() = runBlocking {
        val fresh = MemoryRevisionId("rev-fresh")
        val repo = RecordingMemoryRepository(
            ready = mutableSetOf(fresh),
            stale = mutableSetOf(),
            pdfPageEvidenceByRevision = mapOf(
                fresh to mapOf(1 to MemoryEvidenceId("e1")),
            ),
        )
        val cutover = ApplyMig05EvidenceSearchCutover(
            repo,
            InMemoryMemoryEmbeddingStore(),
            InMemoryMemoryEvidenceEmbeddingStore(),
        )
        val result = cutover.ensureApplied(model, nowEpochMs = 1L)
        assertTrue(result.gapRevisionIds.isEmpty())
        assertEquals(0, result.markedStale)
        assertTrue(fresh in repo.ready)
    }

    private fun summaryRecord(revisionId: MemoryRevisionId) =
        MemoryEmbeddingRecord(
            revisionId = revisionId,
            memoryId = MemoryId("mem-${revisionId.value}"),
            model = model,
            vector = EmbeddingVector(floatArrayOf(1f, 0f)),
            sourceTextFingerprint = "summary-fp-${revisionId.value}",
            createdAtEpochMs = 1L,
        )

    private fun evidenceRecord(revisionId: MemoryRevisionId, evidenceId: MemoryEvidenceId) =
        MemoryEvidenceEmbeddingRecord(
            revisionId = revisionId,
            memoryId = MemoryId("mem-${revisionId.value}"),
            evidenceId = evidenceId,
            model = model,
            vector = EmbeddingVector(floatArrayOf(1f, 0f)),
            sourceTextFingerprint = "fp-${evidenceId.value}",
            createdAtEpochMs = 1L,
        )

    private class RecordingMemoryRepository(
        val ready: MutableSet<MemoryRevisionId>,
        val stale: MutableSet<MemoryRevisionId>,
        private val pdfPageEvidenceByRevision:
            Map<MemoryRevisionId, Map<Int, MemoryEvidenceId>> = emptyMap(),
    ) : MemoryRepository {
        override suspend fun find(
            assetIdentity: AssetIdentity,
            assetFingerprint: AssetFingerprint,
            assemblySchemaVersion: MemoryAssemblySchemaVersion,
        ): Memory? = null

        override suspend fun insert(memory: Memory): MemoryInsertResult =
            MemoryInsertResult.FailedSafely

        override suspend fun countCurrentReady(): Int = ready.size

        override suspend fun countMeaningIndexCandidates(): Int = ready.size + stale.size

        override suspend fun listCurrentReadySummaries(limit: Int) =
            emptyList<MemoryEmbeddingSummary>()

        override suspend fun listMeaningIndexSummaries(limit: Int) =
            emptyList<MemoryEmbeddingSummary>()

        override suspend fun listCurrentReadyRevisionIds(): Set<MemoryRevisionId> =
            ready.toSet()

        override suspend fun listCurrentStaleReindexRevisionIds(): Set<MemoryRevisionId> =
            stale.toSet()

        override suspend fun markIntegrityState(
            revisionIds: Collection<MemoryRevisionId>,
            from: MemoryIntegrityState,
            to: MemoryIntegrityState,
            nowEpochMs: Long,
        ): Int {
            var updated = 0
            for (id in revisionIds) {
                when {
                    from == MemoryIntegrityState.READY &&
                        to == MemoryIntegrityState.STALE_REINDEX_REQUIRED &&
                        ready.remove(id) -> {
                        stale += id
                        updated++
                    }
                    from == MemoryIntegrityState.STALE_REINDEX_REQUIRED &&
                        to == MemoryIntegrityState.READY &&
                        stale.remove(id) -> {
                        ready += id
                        updated++
                    }
                }
            }
            return updated
        }

        override suspend fun findCurrentReadyMeaningLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, MemoryMeaningLookup>()

        override suspend fun findMeaningIndexLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, MemoryMeaningLookup>()

        override suspend fun findPdfPageEvidenceIds(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, Map<Int, MemoryEvidenceId>> =
            pdfPageEvidenceByRevision.filterKeys { it in revisionIds.toSet() }

        override suspend fun findEvidenceSearchRows(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>>()

        override suspend fun findOcrTextEvidenceForEmbedding(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, List<MemoryEvidenceSearchRow>>()

        override suspend fun findNoteTextEvidenceForEmbedding(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, List<MemoryEvidenceSearchRow>>()

        override suspend fun findSignatureAnchors(
            revisionIds: Collection<MemoryRevisionId>,
        ) = emptyMap<MemoryRevisionId, List<com.memora.app.domain.memory.MemoryAnchor>>()
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
