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
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryAnchor
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
import org.junit.Assert.assertFalse
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
    fun empty_summary_and_evidence_index_is_nothing_indexed() =
        runBlocking {
            val engine = FixedEmbeddingEngine(model, dimensions = 4)
            // PdfPageEmbedding* retired (MIG-05 step 4); empty evidence + summary
            // stores must still yield NothingIndexed.
            val outcome = searchUseCase(
                embeddingEngine = engine,
                embeddingStore = InMemoryMemoryEmbeddingStore(),
                evidenceStore = InMemoryMemoryEvidenceEmbeddingStore(),
                cutover = ApplyMig05EvidenceSearchCutover(
                    memoryRepository = FakeMemoryRepository(),
                    embeddingStore = InMemoryMemoryEmbeddingStore(),
                    evidenceEmbeddingStore = InMemoryMemoryEvidenceEmbeddingStore(),
                ),
            )("cafe receipt")
            assertTrue(outcome is MeaningSearchOutcome.NothingIndexed)
        }

    @Test
    fun filler_ask_shape_returns_empty_matches_without_embedding() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val store = InMemoryMemoryEmbeddingStore()
        val revision = MemoryRevisionId("rev-a")
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = revision,
                memoryId = MemoryId("mem-a"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f)),
                sourceTextFingerprint = "fp-a",
                createdAtEpochMs = 1L,
            ),
        )
        val repo = FakeMemoryRepository(
            lookups = mapOf(
                revision to lookup(revision, MemoryId("mem-a"), "Urdu worksheet"),
            ),
        )
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = repo,
        )("show me the files")
        val matches = outcome as MeaningSearchOutcome.Matches
        assertTrue(matches.hits.isEmpty())
        assertEquals(0, engine.embedCalls)
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
        assertFalse(hit.evidenceTokenBoosted)
    }

    @Test
    fun candidate_gen_ranks_by_cosine_without_token_boost() = runBlocking {
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
        assertEquals("memora-open-5page.pdf", hit.label)
        assertEquals("memora-open-3page.pdf", hits[1].label)
        assertFalse(hit.evidenceTokenBoosted)
        assertTrue(hit.score > hits[1].score)
    }

    @Test
    fun repository_failure_returns_failed_outcome() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val store = InMemoryMemoryEmbeddingStore()
        val revision = MemoryRevisionId("rev-fail")
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = revision,
                memoryId = MemoryId("mem-fail"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f)),
                sourceTextFingerprint = "fp",
                createdAtEpochMs = 1L,
            ),
        )
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = ThrowingMemoryRepository(),
        )("invoice")

        assertTrue(outcome is MeaningSearchOutcome.Failed)
        assertEquals("lookup failed", (outcome as MeaningSearchOutcome.Failed).reason)
    }

    @Test
    fun dimension_mismatch_returns_empty_matches_not_failed() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val store = InMemoryMemoryEmbeddingStore()
        val revision = MemoryRevisionId("rev-dim")
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = revision,
                memoryId = MemoryId("mem-dim"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f)),
                sourceTextFingerprint = "fp",
                createdAtEpochMs = 1L,
            ),
        )
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = FakeMemoryRepository(
                lookups = mapOf(
                    revision to lookup(revision, MemoryId("mem-dim"), "invoice.pdf"),
                ),
            ),
        )("invoice")

        val matches = outcome as MeaningSearchOutcome.Matches
        assertTrue(matches.hits.isEmpty())
    }

    /**
     * D-11: the pool is a fixed slice of the corpus and every later stage can
     * only subtract, so a Memory that literally says `silky` must not be
     * truncated away by cosine before precision ever sees it.
     */
    @Test
    fun a_literal_match_outside_the_cosine_pool_is_still_reachable() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val store = InMemoryMemoryEmbeddingStore()
        val lookups = mutableMapOf<MemoryRevisionId, MemoryMeaningLookup>()

        // Four near-perfect cosine neighbours that never say `silky`.
        repeat(4) { index ->
            val revision = MemoryRevisionId("rev-near-$index")
            val memory = MemoryId("mem-near-$index")
            store.upsert(
                MemoryEmbeddingRecord(
                    revisionId = revision,
                    memoryId = memory,
                    model = model,
                    vector = EmbeddingVector(floatArrayOf(1f, 0.05f * index, 0f)),
                    sourceTextFingerprint = "fp-near-$index",
                    createdAtEpochMs = index.toLong(),
                ),
            )
            lookups[revision] = lookup(
                revisionId = revision,
                memoryId = memory,
                label = "bus-rules-$index.pdf",
                summaryText = "Bus discipline rules for students",
            )
        }

        // The one Memory that does say it, far away in vector space.
        val target = MemoryRevisionId("rev-silky")
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = target,
                memoryId = MemoryId("mem-silky"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(0.2f, 1f, 0f)),
                sourceTextFingerprint = "fp-silky",
                createdAtEpochMs = 99L,
            ),
        )
        lookups[target] = lookup(
            revisionId = target,
            memoryId = MemoryId("mem-silky"),
            label = "spelling-list.pdf",
            summaryText = "Irregular consonants anchor silky wreck",
        )

        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = FakeMemoryRepository(lookups = lookups),
        )("silky", 2)

        val hits = (outcome as MeaningSearchOutcome.Matches).hits
        assertEquals(2, hits.size)
        // Reachability, not rank: admission is this class's job, ranking is not.
        assertTrue(hits.any { it.label == "spelling-list.pdf" })
        // The pool is still handed back in cosine order.
        assertEquals(hits.sortedByDescending { it.score }, hits)
    }

    /**
     * Admission prefers literal matches but must not invent a precision gate of
     * its own: with nothing matching, candidate generation still hands meaning
     * neighbours downstream and lets the lexical precision gate decide.
     */
    /**
     * D-15: after D-12, a two-word cue with no exact AND still has a partial
     * tier. Admission must reserve a seat for a Memory that carries *some* of
     * the named words, not only one that carries all of them — otherwise
     * `swimming schedule` depends on cosine luck the same way D-11 did.
     */
    @Test
    fun a_partial_literal_match_outside_the_cosine_pool_is_still_reachable() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val store = InMemoryMemoryEmbeddingStore()
        val lookups = mutableMapOf<MemoryRevisionId, MemoryMeaningLookup>()

        repeat(4) { index ->
            val revision = MemoryRevisionId("rev-near-$index")
            val memory = MemoryId("mem-near-$index")
            store.upsert(
                MemoryEmbeddingRecord(
                    revisionId = revision,
                    memoryId = memory,
                    model = model,
                    vector = EmbeddingVector(floatArrayOf(1f, 0.05f * index, 0f)),
                    sourceTextFingerprint = "fp-near-$index",
                    createdAtEpochMs = index.toLong(),
                ),
            )
            lookups[revision] = lookup(
                revisionId = revision,
                memoryId = memory,
                label = "bus-rules-$index.pdf",
                summaryText = "Bus discipline rules for students",
            )
        }

        val target = MemoryRevisionId("rev-swim")
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = target,
                memoryId = MemoryId("mem-swim"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(0.2f, 1f, 0f)),
                sourceTextFingerprint = "fp-swim",
                createdAtEpochMs = 99L,
            ),
        )
        lookups[target] = lookup(
            revisionId = target,
            memoryId = MemoryId("mem-swim"),
            label = "Grade-2-Swimming-TT-2026.pdf",
            summaryText = "Grade 2 Swimming Timetable 2026 PERIOD TIME MON TUE",
        )

        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = FakeMemoryRepository(lookups = lookups),
        )("swimming schedule", 2)

        val hits = (outcome as MeaningSearchOutcome.Matches).hits
        assertEquals(2, hits.size)
        assertTrue(hits.any { it.label == "Grade-2-Swimming-TT-2026.pdf" })
        assertEquals(hits.sortedByDescending { it.score }, hits)
    }

    /**
     * Deeper coverage claims the scarce seat: an exact match far from the
     * query vector beats a nearer one-word partial. Admission still returns
     * cosine order; this test uses a pool of one so order is vacuous.
     */
    @Test
    fun an_exact_match_claims_a_seat_before_a_partial() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val store = InMemoryMemoryEmbeddingStore()
        val lookups = mutableMapOf<MemoryRevisionId, MemoryMeaningLookup>()

        repeat(4) { index ->
            val revision = MemoryRevisionId("rev-partial-$index")
            val memory = MemoryId("mem-partial-$index")
            store.upsert(
                MemoryEmbeddingRecord(
                    revisionId = revision,
                    memoryId = memory,
                    model = model,
                    vector = EmbeddingVector(floatArrayOf(1f, 0.05f * index, 0f)),
                    sourceTextFingerprint = "fp-partial-$index",
                    createdAtEpochMs = index.toLong(),
                ),
            )
            lookups[revision] = lookup(
                revisionId = revision,
                memoryId = memory,
                label = "swim-only-$index.pdf",
                summaryText = "Grade 2 swimming lessons",
            )
        }

        val exact = MemoryRevisionId("rev-exact")
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = exact,
                memoryId = MemoryId("mem-exact"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(0.2f, 1f, 0f)),
                sourceTextFingerprint = "fp-exact",
                createdAtEpochMs = 99L,
            ),
        )
        lookups[exact] = lookup(
            revisionId = exact,
            memoryId = MemoryId("mem-exact"),
            label = "swimming-schedule.pdf",
            summaryText = "Swimming schedule term 2",
        )

        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = FakeMemoryRepository(lookups = lookups),
        )("swimming schedule", 1)

        val hits = (outcome as MeaningSearchOutcome.Matches).hits
        assertEquals(listOf("swimming-schedule.pdf"), hits.map { it.label })
    }

    @Test
    fun a_head_family_keeps_a_seat_when_qualifier_docs_fill_cosine() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val store = InMemoryMemoryEmbeddingStore()
        val lookups = mutableMapOf<MemoryRevisionId, MemoryMeaningLookup>()

        repeat(4) { index ->
            val revision = MemoryRevisionId("rev-school-$index")
            val memory = MemoryId("mem-school-$index")
            store.upsert(
                MemoryEmbeddingRecord(
                    revisionId = revision,
                    memoryId = memory,
                    model = model,
                    vector = EmbeddingVector(floatArrayOf(1f, 0.05f * index, 0f)),
                    sourceTextFingerprint = "fp-school-$index",
                    createdAtEpochMs = index.toLong(),
                ),
            )
            lookups[revision] = lookup(
                revisionId = revision,
                memoryId = memory,
                label = "grade-report-$index.pdf",
                summaryText = "term classes grade report $index",
            )
        }

        val gold = MemoryRevisionId("rev-tt")
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = gold,
                memoryId = MemoryId("mem-tt"),
                model = model,
                vector = EmbeddingVector(floatArrayOf(0.2f, 1f, 0f)),
                sourceTextFingerprint = "fp-tt",
                createdAtEpochMs = 99L,
            ),
        )
        lookups[gold] = lookup(
            revisionId = gold,
            memoryId = MemoryId("mem-tt"),
            label = "Grade-2-Swimming-TT-2026.pdf",
            summaryText = "grade 2 weekly swimming timetable",
        )

        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = FakeMemoryRepository(lookups = lookups),
        )("when are the swimming classes for grade 2", 2)

        val hits = (outcome as MeaningSearchOutcome.Matches).hits
        assertEquals(2, hits.size)
        assertTrue(hits.any { it.label == "Grade-2-Swimming-TT-2026.pdf" })
        assertEquals(hits.sortedByDescending { it.score }, hits)
    }

    @Test
    fun a_cue_with_no_literal_match_still_offers_meaning_neighbours() = runBlocking {
        val engine = FixedEmbeddingEngine(model, dimensions = 3)
        val store = InMemoryMemoryEmbeddingStore()
        val revision = MemoryRevisionId("rev-near")
        val memory = MemoryId("mem-near")
        store.upsert(
            MemoryEmbeddingRecord(
                revisionId = revision,
                memoryId = memory,
                model = model,
                vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f)),
                sourceTextFingerprint = "fp-near",
                createdAtEpochMs = 1L,
            ),
        )
        engine.nextQueryVector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        val outcome = searchUseCase(
            embeddingEngine = engine,
            embeddingStore = store,
            memoryRepository = FakeMemoryRepository(
                lookups = mapOf(
                    revision to lookup(
                        revisionId = revision,
                        memoryId = memory,
                        label = "bus-rules.pdf",
                        summaryText = "Bus discipline rules for students",
                    ),
                ),
            ),
        )("silky", 5)

        val hits = (outcome as MeaningSearchOutcome.Matches).hits
        assertEquals(1, hits.size)
        assertEquals("bus-rules.pdf", hits.first().label)
    }

    @Test
    fun does_not_import_saved_pdf_page_text_source() {
        val imports = SearchAssetMemoriesByMeaning::class.java.declaredConstructors
            .flatMap { it.parameterTypes.toList() }
            .map { it.name }
        assertTrue(imports.none { it.contains("SavedPdfPageTextSource") })
        // PdfPageEmbeddingStore type retired in MIG-05 step 4 — compile-level absence.
        assertTrue(imports.none { it.contains("PdfPageEmbedding") })
        assertTrue(imports.any { it.contains("MemoryEvidenceEmbeddingStore") })
    }

    private fun searchUseCase(
        embeddingEngine: EmbeddingEngine,
        embeddingStore: MemoryEmbeddingStore = InMemoryMemoryEmbeddingStore(),
        evidenceStore: MemoryEvidenceEmbeddingStore = InMemoryMemoryEvidenceEmbeddingStore(),
        memoryRepository: MemoryRepository = FakeMemoryRepository(),
        cutover: ApplyMig05EvidenceSearchCutover = ApplyMig05EvidenceSearchCutover(
            memoryRepository = memoryRepository,
            embeddingStore = embeddingStore,
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
        summaryText: String = "$label summary text for evidence",
    ) = MemoryMeaningLookup(
        revisionId = revisionId,
        memoryId = memoryId,
        sourceId = SourceId("source"),
        sourceAssetKey = SourceAssetKey(label),
        assetType = AssetType.PDF,
        displayLabel = label,
        summaryText = summaryText,
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

        override fun embedText(text: String): EmbeddingEncodeResult {
            embedCalls += 1
            return EmbeddingEncodeResult.Success(nextQueryVector, model)
        }

        var embedCalls: Int = 0
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

    private open class FakeMemoryRepository(
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

        override suspend fun countMeaningIndexPending(
            model: com.memora.app.domain.intelligence.ModelVersionIdentity,
        ): Int = lookups.size

        override suspend fun listMeaningIndexSummaries(
            model: com.memora.app.domain.intelligence.ModelVersionIdentity,
            limit: Int,
        ) = emptyList<MemoryEmbeddingSummary>()

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

        override suspend fun findMeaningIndexLookups(
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

        override suspend fun findOcrTextEvidenceForEmbedding(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>> = emptyMap()

        override suspend fun findNoteTextEvidenceForEmbedding(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, List<MemoryEvidenceSearchRow>> = emptyMap()

        override suspend fun findSignatureAnchors(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, List<MemoryAnchor>> = emptyMap()
    }

    private class ThrowingMemoryRepository : FakeMemoryRepository() {
        override suspend fun findMeaningIndexLookups(
            revisionIds: Collection<MemoryRevisionId>,
        ): Map<MemoryRevisionId, MemoryMeaningLookup> =
            throw IllegalStateException("lookup failed")
    }
}
