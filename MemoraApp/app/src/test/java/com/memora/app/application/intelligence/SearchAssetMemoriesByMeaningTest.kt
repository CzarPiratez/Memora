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
import com.memora.app.domain.intelligence.PdfPageEmbeddingRecord
import com.memora.app.domain.intelligence.PdfPageEmbeddingStore
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
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAssetMemoriesByMeaningTest {
    private val model = ModelVersionIdentity("test-embedder", "1")

    @Test
    fun unavailable_engine_returns_unavailable_without_ranking() = runBlocking {
        val outcome = searchUseCase(
            embeddingEngine = UnavailableEmbeddingEngine("model missing"),
        )("cafe receipt")
        assertTrue(outcome is MeaningSearchOutcome.EngineUnavailable)
    }

    @Test
    fun empty_summary_and_evidence_index_is_nothing_indexed_even_if_pdf_page_store_has_rows() =
        runBlocking {
            val engine = FixedEmbeddingEngine(model, dimensions = 4)
            val orphanPageStore = InMemoryPdfPageEmbeddingStore()
            orphanPageStore.upsert(
                PdfPageEmbeddingRecord(
                    revisionId = MemoryRevisionId("rev-orphan"),
                    memoryId = MemoryId("mem-orphan"),
                    pageNumber = 1,
                    model = model,
                    vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f, 0f)),
                    sourceTextFingerprint = "fp-orphan",
                    createdAtEpochMs = 1L,
                ),
            )
            // Search no longer reads PdfPageEmbeddingStore; orphan page rows must not
            // prevent NothingIndexed when summary + evidence stores are empty.
            val outcome = searchUseCase(
                embeddingEngine = engine,
                embeddingStore = InMemoryMemoryEmbeddingStore(),
                evidenceStore = InMemoryMemoryEvidenceEmbeddingStore(),
                cutover = ApplyMig05EvidenceSearchCutover(
                    memoryRepository = FakeMemoryRepository(),
                    pdfPageEmbeddingStore = orphanPageStore,
                    evidenceEmbeddingStore = InMemoryMemoryEvidenceEmbeddingStore(),
                ),
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
                cafeRevision to lookup(
                    cafeRevision,
                    MemoryId("mem-cafe"),
                    "Cafe receipt",
                    citedPdfPageNumber = 3,
                ),
                unrelatedRevision to lookup(
                    unrelatedRevision,
                    MemoryId("mem-other"),
                    "Unrelated note",
                ),
            ),
        )
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(0.9f, 0.1f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = repo,
        )("coffee shop bill")
        val matches = outcome as MeaningSearchOutcome.Matches
        assertEquals(2, matches.hits.size)
        assertEquals("Cafe receipt", matches.hits.first().label)
        assertTrue(matches.hits.first().score > matches.hits.last().score)
        assertEquals(3, matches.hits.first().citedPdfPageNumber)
        assertEquals(null, matches.hits.last().citedPdfPageNumber)
    }

    @Test
    fun prefers_indexed_evidence_page_over_weaker_summary() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val summaryStore = InMemoryMemoryEmbeddingStore()
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val revision = MemoryRevisionId("rev-pdf")
        val evidenceId = MemoryEvidenceId("e3")
        summaryStore.upsert(
            MemoryEmbeddingRecord(
                revisionId = revision,
                memoryId = MemoryId("mem-pdf"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(0f, 1f, 0f)),
                sourceTextFingerprint = "fp-sum",
                createdAtEpochMs = 1L,
            ),
        )
        evidenceStore.upsert(
            MemoryEvidenceEmbeddingRecord(
                revisionId = revision,
                memoryId = MemoryId("mem-pdf"),
                evidenceId = evidenceId,
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f)),
                sourceTextFingerprint = "fp-page3",
                createdAtEpochMs = 2L,
            ),
        )
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = summaryStore,
            evidenceStore = evidenceStore,
            memoryRepository = FakeMemoryRepository(
                lookups = mapOf(
                    revision to lookup(revision, MemoryId("mem-pdf"), "memora-open-3page.pdf"),
                ),
                evidenceRows = mapOf(
                    revision to mapOf(
                        evidenceId to MemoryEvidenceSearchRow(
                            revisionId = revision,
                            evidenceId = evidenceId,
                            locator = PdfPageEvidenceLocator.formatLocator(3),
                            excerpt = "Page 3 ECHO meet mira follow-up",
                        ),
                    ),
                ),
            ),
        )("mira")
        val hit = (outcome as MeaningSearchOutcome.Matches).hits.single()
        assertEquals(3, hit.rankedPdfPageNumber)
        assertTrue(hit.summaryText.contains("mira"))
        assertTrue(hit.evidenceTokenBoosted)
    }

    @Test
    fun evidence_token_boost_outranks_foxtrot_without_token() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val evidenceStore = InMemoryMemoryEvidenceEmbeddingStore()
        val foxtrot = MemoryRevisionId("rev-5")
        val miraPage = MemoryRevisionId("rev-3")
        val foxtrotEvidence = MemoryEvidenceId("e1")
        val miraEvidence = MemoryEvidenceId("e3")
        evidenceStore.upsert(
            MemoryEvidenceEmbeddingRecord(
                revisionId = foxtrot,
                memoryId = MemoryId("mem-5"),
                evidenceId = foxtrotEvidence,
                model = model,
                vector = EmbeddingVector(floatArrayOf(0.4f, 0.6f, 0f)),
                sourceTextFingerprint = "fp-f",
                createdAtEpochMs = 1L,
            ),
        )
        evidenceStore.upsert(
            MemoryEvidenceEmbeddingRecord(
                revisionId = miraPage,
                memoryId = MemoryId("mem-3"),
                evidenceId = miraEvidence,
                model = model,
                vector = EmbeddingVector(floatArrayOf(0.2f, 0.8f, 0f)),
                sourceTextFingerprint = "fp-m",
                createdAtEpochMs = 2L,
            ),
        )
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            evidenceStore = evidenceStore,
            memoryRepository = FakeMemoryRepository(
                lookups = mapOf(
                    foxtrot to lookup(foxtrot, MemoryId("mem-5"), "memora-open-5page.pdf"),
                    miraPage to lookup(miraPage, MemoryId("mem-3"), "memora-open-3page.pdf"),
                ),
                evidenceRows = mapOf(
                    foxtrot to mapOf(
                        foxtrotEvidence to MemoryEvidenceSearchRow(
                            revisionId = foxtrot,
                            evidenceId = foxtrotEvidence,
                            locator = PdfPageEvidenceLocator.formatLocator(1),
                            excerpt = "Page 1 FOXTROT cover sheet",
                        ),
                    ),
                    miraPage to mapOf(
                        miraEvidence to MemoryEvidenceSearchRow(
                            revisionId = miraPage,
                            evidenceId = miraEvidence,
                            locator = PdfPageEvidenceLocator.formatLocator(3),
                            excerpt = "Page 3 ECHO meet mira follow-up",
                        ),
                    ),
                ),
            ),
        )("mira")
        val hits = (outcome as MeaningSearchOutcome.Matches).hits
        val hit = hits.first()
        assertEquals("memora-open-3page.pdf", hit.label)
        assertEquals(3, hit.rankedPdfPageNumber)
        assertTrue(hit.evidenceTokenBoosted)
        assertEquals("memora-open-5page.pdf", hits[1].label)
        assertTrue(hit.score > hits[1].score)
    }

    @Test
    fun does_not_import_saved_pdf_page_text_source() {
        val imports = SearchAssetMemoriesByMeaning::class.java.declaredConstructors
            .flatMap { it.parameterTypes.toList() }
            .map { it.name }
        assertTrue(imports.none { it.contains("SavedPdfPageTextSource") })
        assertTrue(imports.none { it.contains("PdfPageEmbeddingStore") })
        assertTrue(imports.any { it.contains("MemoryEvidenceEmbeddingStore") })
    }

    private fun searchUseCase(
        embeddingEngine: EmbeddingEngine,
        embeddingStore: MemoryEmbeddingStore = InMemoryMemoryEmbeddingStore(),
        evidenceStore: MemoryEvidenceEmbeddingStore = InMemoryMemoryEvidenceEmbeddingStore(),
        memoryRepository: MemoryRepository = FakeMemoryRepository(),
        cutover: ApplyMig05EvidenceSearchCutover = ApplyMig05EvidenceSearchCutover(
            memoryRepository = memoryRepository,
            pdfPageEmbeddingStore = InMemoryPdfPageEmbeddingStore(),
            evidenceEmbeddingStore = evidenceStore,
        ),
    ) = SearchAssetMemoriesByMeaning(
        embeddingEngine = embeddingEngine,
        embeddingStore = embeddingStore,
        evidenceEmbeddingStore = evidenceStore,
        memoryRepository = memoryRepository,
        applyMig05EvidenceSearchCutover = cutover,
    )

    private fun lookup(
        revisionId: MemoryRevisionId,
        memoryId: MemoryId,
        label: String,
        citedPdfPageNumber: Int? = null,
    ) = MemoryMeaningLookup(
        revisionId = revisionId,
        memoryId = memoryId,
        sourceId = SourceId("source"),
        sourceAssetKey = SourceAssetKey(label),
        assetType = AssetType.PDF,
        displayLabel = label,
        summaryText = "$label summary text for evidence",
        citedPdfPageNumber = citedPdfPageNumber,
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

    private class InMemoryPdfPageEmbeddingStore : PdfPageEmbeddingStore {
        private val records = linkedMapOf<String, PdfPageEmbeddingRecord>()

        private fun key(
            revisionId: MemoryRevisionId,
            pageNumber: Int,
            model: ModelVersionIdentity,
        ) = "${revisionId.value}|$pageNumber|${model.modelId}|${model.version}"

        override fun find(
            revisionId: MemoryRevisionId,
            pageNumber: Int,
            model: ModelVersionIdentity,
        ): PdfPageEmbeddingRecord? = records[key(revisionId, pageNumber, model)]

        override fun upsert(record: PdfPageEmbeddingRecord) {
            records[key(record.revisionId, record.pageNumber, record.model)] = record
        }

        override fun countForModel(model: ModelVersionIdentity): Int =
            records.values.count {
                it.model.modelId == model.modelId && it.model.version == model.version
            }

        override fun listForModel(model: ModelVersionIdentity): List<PdfPageEmbeddingRecord> =
            records.values.filter {
                it.model.modelId == model.modelId && it.model.version == model.version
            }
    }

    private class FakeMemoryRepository(
        private val lookups: Map<MemoryRevisionId, MemoryMeaningLookup> = emptyMap(),
        private val evidenceRows: Map<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>> =
            emptyMap(),
        private val readyRevisionIds: Set<MemoryRevisionId> = lookups.keys,
    ) : MemoryRepository {
        override suspend fun find(
            assetIdentity: AssetIdentity,
            assetFingerprint: AssetFingerprint,
            assemblySchemaVersion: MemoryAssemblySchemaVersion,
        ): Memory? = null

        override suspend fun insert(memory: Memory): MemoryInsertResult =
            MemoryInsertResult.FailedSafely

        override suspend fun countCurrentReady(): Int = lookups.size

        override suspend fun countMeaningIndexCandidates(): Int = lookups.size

        override suspend fun listCurrentReadySummaries(limit: Int) =
            emptyList<MemoryEmbeddingSummary>()

        override suspend fun listMeaningIndexSummaries(limit: Int) =
            emptyList<MemoryEmbeddingSummary>()

        override suspend fun listCurrentReadyRevisionIds(): Set<MemoryRevisionId> =
            readyRevisionIds

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
        ): Map<MemoryRevisionId, MemoryMeaningLookup> =
            lookups.filterKeys { it in revisionIds }

        override suspend fun findPdfPageEvidenceIds(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, Map<Int, MemoryEvidenceId>> = emptyMap()

        override suspend fun findEvidenceSearchRows(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>> =
            evidenceRows.filterKeys { it in revisionIds }
    }
}
