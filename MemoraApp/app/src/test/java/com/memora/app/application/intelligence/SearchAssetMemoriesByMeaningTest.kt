package com.memora.app.application.intelligence

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
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.PdfPageEmbeddingRecord
import com.memora.app.domain.intelligence.PdfPageEmbeddingStore
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
            pdfPageEmbeddingStore = InMemoryPdfPageEmbeddingStore(),
            savedPdfPages = EmptySavedPdfPages(),
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
            pdfPageEmbeddingStore = InMemoryPdfPageEmbeddingStore(),
            savedPdfPages = EmptySavedPdfPages(),
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
        val outcome = SearchAssetMemoriesByMeaning(
            embeddingEngine = engine,
            embeddingStore = store,
            pdfPageEmbeddingStore = InMemoryPdfPageEmbeddingStore(),
            savedPdfPages = EmptySavedPdfPages(),
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
    fun prefers_indexed_pdf_page_over_weaker_summary() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val summaryStore = InMemoryMemoryEmbeddingStore()
        val pageStore = InMemoryPdfPageEmbeddingStore()
        val revision = MemoryRevisionId("rev-pdf")
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
        pageStore.upsert(
            PdfPageEmbeddingRecord(
                revisionId = revision,
                memoryId = MemoryId("mem-pdf"),
                pageNumber = 3,
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f)),
                sourceTextFingerprint = "fp-page3",
                createdAtEpochMs = 2L,
            ),
        )
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = SearchAssetMemoriesByMeaning(
            embeddingEngine = engine,
            embeddingStore = summaryStore,
            pdfPageEmbeddingStore = pageStore,
            savedPdfPages = FixedSavedPdfPages(
                listOf(SavedPdfPageText(3, "Page 3 ECHO meet mira follow-up")),
            ),
            memoryRepository = FakeMemoryRepository(
                lookups = mapOf(
                    revision to lookup(revision, MemoryId("mem-pdf"), "memora-open-3page.pdf"),
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
        val pageStore = InMemoryPdfPageEmbeddingStore()
        val foxtrot = MemoryRevisionId("rev-5")
        val miraPage = MemoryRevisionId("rev-3")
        // Query axis (1,0,0): FOXTROT cosine ~0.55 (no cue token); mira ~0.24
        // then +TOKEN_BOOST outranks FOXTROT. Perfect cosine=1.0 cannot be
        // flipped by a bounded 0.35 assist — that was a broken fixture.
        pageStore.upsert(
            PdfPageEmbeddingRecord(
                revisionId = foxtrot,
                memoryId = MemoryId("mem-5"),
                pageNumber = 1,
                model = model,
                vector = EmbeddingVector(floatArrayOf(0.4f, 0.6f, 0f)),
                sourceTextFingerprint = "fp-f",
                createdAtEpochMs = 1L,
            ),
        )
        pageStore.upsert(
            PdfPageEmbeddingRecord(
                revisionId = miraPage,
                memoryId = MemoryId("mem-3"),
                pageNumber = 3,
                model = model,
                vector = EmbeddingVector(floatArrayOf(0.2f, 0.8f, 0f)),
                sourceTextFingerprint = "fp-m",
                createdAtEpochMs = 2L,
            ),
        )
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = SearchAssetMemoriesByMeaning(
            embeddingEngine = engine,
            embeddingStore = InMemoryMemoryEmbeddingStore(),
            pdfPageEmbeddingStore = pageStore,
            savedPdfPages = object : SavedPdfPageTextSource {
                override suspend fun listCurrentVerifiedPages(
                    sourceId: String,
                    sourceAssetKey: String,
                ): List<SavedPdfPageText> = when {
                    sourceAssetKey.contains("5page") ->
                        listOf(SavedPdfPageText(1, "Page 1 FOXTROT cover sheet"))
                    else -> listOf(SavedPdfPageText(3, "Page 3 ECHO meet mira follow-up"))
                }
            },
            memoryRepository = FakeMemoryRepository(
                lookups = mapOf(
                    foxtrot to lookup(foxtrot, MemoryId("mem-5"), "memora-open-5page.pdf"),
                    miraPage to lookup(miraPage, MemoryId("mem-3"), "memora-open-3page.pdf"),
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

    private class EmptySavedPdfPages : SavedPdfPageTextSource {
        override suspend fun listCurrentVerifiedPages(
            sourceId: String,
            sourceAssetKey: String,
        ): List<SavedPdfPageText> = emptyList()
    }

    private class FixedSavedPdfPages(
        private val pages: List<SavedPdfPageText>,
    ) : SavedPdfPageTextSource {
        override suspend fun listCurrentVerifiedPages(
            sourceId: String,
            sourceAssetKey: String,
        ): List<SavedPdfPageText> = pages
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
